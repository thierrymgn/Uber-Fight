import {
  LogLevel,
  LogEntry,
  LogAttributeValue,
  OTLPAttribute,
  OTLPLogRecord,
  OTLPLogsPayload,
  SEVERITY_NUMBER,
  GrafanaConfig,
} from "./types";

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
    serviceName: "uber-fight-backend",
    serviceVersion: "1.0.0",
    environment: process.env.NEXT_PUBLIC_APP_ENV || process.env.NODE_ENV || "development",
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
    "auth",
    "credit_card",
    "creditCard",
    "card_number",
    "cardNumber",
    "cvv",
    "ssn",
    "social_security",
    "private_key",
    "privateKey",
  ];

  const sanitized: Record<string, LogAttributeValue> = {};

  for (const [key, value] of Object.entries(attributes)) {
    const lowerKey = key.toLowerCase();
    const isSensitive = sensitiveKeys.some(
      (sensitive) =>
        lowerKey.includes(sensitive.toLowerCase())
    );

    if (isSensitive) {
      sanitized[key] = "[REDACTED]";
    } else {
      sanitized[key] = value;
    }
  }

  return sanitized;
}

function createLogRecord(entry: LogEntry): OTLPLogRecord {
  const level = entry.level || "info";
  const timestamp = entry.timestamp
    ? new Date(entry.timestamp).getTime() * 1e6
    : Date.now() * 1e6;

  const record: OTLPLogRecord = {
    timeUnixNano: String(timestamp),
    severityText: level.toUpperCase() as "INFO" | "WARN" | "ERROR" | "DEBUG",
    severityNumber: SEVERITY_NUMBER[level],
    body: { stringValue: entry.message },
  };

  if (entry.attributes && Object.keys(entry.attributes).length > 0) {
    const sanitized = sanitizeAttributes(entry.attributes);
    record.attributes = attributesToOTLP(sanitized);
  }

  return record;
}

function createOTLPPayload(logRecords: OTLPLogRecord[]): OTLPLogsPayload {
  const config = getConfig();

  return {
    resourceLogs: [
      {
        resource: {
          attributes: [
            { key: "service.name", value: { stringValue: config.serviceName } },
            {
              key: "service.version",
              value: { stringValue: config.serviceVersion },
            },
            { key: "environment", value: { stringValue: config.environment } },
            { key: "deployment.platform", value: { stringValue: "vercel" } },
          ],
        },
        scopeLogs: [
          {
            scope: {
              name: "uber-fight-logger",
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

export async function sendLog(
  message: string,
  level: LogLevel = "info",
  attributes?: Record<string, LogAttributeValue>
): Promise<void> {
  try {
    const config = getConfig();

    if (!config.instanceId || !config.apiKey) {
      console.warn(
        "[GrafanaLogger] Configuration manquante - GRAFANA_INSTANCE_ID ou GRAFANA_API_KEY non définis"
      );
      return;
    }

    const logRecord = createLogRecord({
      message,
      level,
      attributes,
    });

    const payload = createOTLPPayload([logRecord]);

    const response = await fetch(config.endpoint, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errorText = await response.text().catch(() => "Unknown error");
      console.error(
        `[GrafanaLogger] Échec envoi log: ${response.status} - ${errorText}`
      );
    }
  } catch (error) {
    console.error("[GrafanaLogger] Erreur lors de l'envoi du log:", error);
  }
}

export async function sendBatchLogs(logs: LogEntry[]): Promise<void> {
  try {
    if (logs.length === 0) {
      return;
    }

    const config = getConfig();

    if (!config.instanceId || !config.apiKey) {
      console.warn(
        "[GrafanaLogger] Configuration manquante - GRAFANA_INSTANCE_ID ou GRAFANA_API_KEY non définis"
      );
      return;
    }

    const logRecords = logs.map(createLogRecord);
    const payload = createOTLPPayload(logRecords);

    const response = await fetch(config.endpoint, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errorText = await response.text().catch(() => "Unknown error");
      console.error(
        `[GrafanaLogger] Échec envoi batch: ${response.status} - ${errorText}`
      );
    }
  } catch (error) {
    console.error("[GrafanaLogger] Erreur lors de l'envoi du batch:", error);
  }
}

// ============================================================================

export async function logInfo(
  message: string,
  attributes?: Record<string, LogAttributeValue>
): Promise<void> {
  return sendLog(message, "info", attributes);
}

export async function logWarn(
  message: string,
  attributes?: Record<string, LogAttributeValue>
): Promise<void> {
  return sendLog(message, "warn", attributes);
}

export async function logError(
  message: string,
  attributes?: Record<string, LogAttributeValue>
): Promise<void> {
  return sendLog(message, "error", attributes);
}

export async function logDebug(
  message: string,
  attributes?: Record<string, LogAttributeValue>
): Promise<void> {
  return sendLog(message, "debug", attributes);
}

export type { LogLevel, LogEntry, LogAttributeValue, GrafanaConfig };
