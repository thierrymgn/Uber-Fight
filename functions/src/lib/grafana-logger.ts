import {
  LogLevel,
  LogAttributeValue,
  OTLPAttribute,
  OTLPLogRecord,
  OTLPLogsPayload,
  SEVERITY_NUMBER,
  GrafanaConfig,
} from "./grafana-types";

// ============================================================================
// CONF
// ============================================================================
function getConfig(): GrafanaConfig {
  return {
    instanceId: process.env.GRAFANA_INSTANCE_ID || "",
    apiKey: process.env.GRAFANA_API_KEY || "",
    endpoint:
      process.env.GRAFANA_OTLP_ENDPOINT ||
      "https://otlp-gateway-prod-gb-south-1.grafana.net/otlp/v1/logs",
    serviceName: "uber-fight-functions",
    serviceVersion: "1.0.0",
    environment: process.env.FUNCTIONS_ENV || "production",
  };
}

function getAuthHeaders(): Record<string, string> {
  const config = getConfig();
  const authPair = `${config.instanceId}:${config.apiKey}`;
  const encoded = Buffer.from(authPair).toString("base64");

  return {
    "Content-Type": "application/json",
    Authorization: `Basic ${encoded}`,
  };
}

// ============================================================================
// UTILS
// ============================================================================

function toOTLPAttribute(key: string, value: LogAttributeValue): OTLPAttribute {
  if (value === null || value === undefined) {
    return { key, value: { stringValue: "" } };
  }

  if (typeof value === "boolean") {
    return { key, value: { boolValue: value } };
  }

  if (typeof value === "number") {
    if (Number.isInteger(value)) {
      return { key, value: { intValue: value } };
    }
    return { key, value: { stringValue: String(value) } };
  }

  return { key, value: { stringValue: String(value) } };
}

function attributesToOTLP(
  attributes: Record<string, LogAttributeValue>
): OTLPAttribute[] {
  return Object.entries(attributes).map(([key, value]) =>
    toOTLPAttribute(key, value)
  );
}

function sanitizeAttributes(
  attributes: Record<string, LogAttributeValue>
): Record<string, LogAttributeValue> {
  const sensitiveKeys = [
    "password",
    "token",
    "secret",
    "apiKey",
    "api_key",
    "authorization",
    "credit_card",
    "cvv",
    "ssn",
    "private_key",
  ];

  const sanitized: Record<string, LogAttributeValue> = {};

  for (const [key, value] of Object.entries(attributes)) {
    const lowerKey = key.toLowerCase();
    const isSensitive = sensitiveKeys.some((s) => lowerKey.includes(s.toLowerCase()));

    if (isSensitive) {
      sanitized[key] = "[REDACTED]";
    } else {
      sanitized[key] = value;
    }
  }

  return sanitized;
}

function createLogRecord(
  message: string,
  level: LogLevel,
  attributes?: Record<string, LogAttributeValue>
): OTLPLogRecord {
  const timestamp = Date.now() * 1e6;

  const record: OTLPLogRecord = {
    timeUnixNano: String(timestamp),
    severityText: level.toUpperCase() as "INFO" | "WARN" | "ERROR" | "DEBUG",
    severityNumber: SEVERITY_NUMBER[level],
    body: { stringValue: message },
  };

  if (attributes && Object.keys(attributes).length > 0) {
    const sanitized = sanitizeAttributes(attributes);
    record.attributes = attributesToOTLP(sanitized);
  }

  return record;
}

function createOTLPPayload(
  logRecords: OTLPLogRecord[],
  functionName?: string
): OTLPLogsPayload {
  const config = getConfig();

  const resourceAttributes: OTLPAttribute[] = [
    { key: "service.name", value: { stringValue: config.serviceName } },
    { key: "service.version", value: { stringValue: config.serviceVersion } },
    { key: "environment", value: { stringValue: config.environment } },
    { key: "deployment.platform", value: { stringValue: "firebase-functions" } },
    { key: "cloud.region", value: { stringValue: process.env.FUNCTION_REGION || "europe-west1" } },
  ];

  if (functionName) {
    resourceAttributes.push({
      key: "faas.name",
      value: { stringValue: functionName },
    });
  }

  return {
    resourceLogs: [
      {
        resource: {
          attributes: resourceAttributes,
        },
        scopeLogs: [
          {
            scope: {
              name: "uber-fight-functions-logger",
              version: "1.0.0",
            },
            logRecords,
          },
        ],
      },
    ],
  };
}

// ============================================================================
// MAIN LOG FUNCTION
// ============================================================================

export async function logFunction(
  functionName: string,
  message: string,
  level: LogLevel = "info",
  context?: Record<string, LogAttributeValue>
): Promise<void> {
  try {
    const config = getConfig();

    if (!config.instanceId || !config.apiKey) {
      console.warn(
        `[GrafanaLogger] Configuration manquante - GRAFANA_INSTANCE_ID ou GRAFANA_API_KEY non définis`
      );
      console.log(`[${functionName}] [${level.toUpperCase()}] ${message}`, context || "");
      return;
    }

    const logRecord = createLogRecord(message, level, {
      functionName,
      ...context,
    });

    const payload = createOTLPPayload([logRecord], functionName);

    const response = await fetch(config.endpoint, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errorText = await response.text().catch(() => "Unknown error");
      console.error(`[GrafanaLogger] Échec envoi: ${response.status} - ${errorText}`);
    }
  } catch (error) {
    console.error("[GrafanaLogger] Erreur:", error);
  }
}

export type { LogLevel, LogAttributeValue };
