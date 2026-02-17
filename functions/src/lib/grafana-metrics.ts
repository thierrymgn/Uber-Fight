import type { LogAttributeValue, OTLPAttribute } from "./grafana-types";

interface OTLPNumberDataPoint {
  attributes?: OTLPAttribute[];
  startTimeUnixNano: string;
  timeUnixNano: string;
  asInt?: number;
  asDouble?: number;
}

interface OTLPMetric {
  name: string;
  unit?: string;
  sum?: {
    dataPoints: OTLPNumberDataPoint[];
    aggregationTemporality: number;
    isMonotonic: boolean;
  };
  gauge?: {
    dataPoints: OTLPNumberDataPoint[];
  };
}

interface OTLPMetricsPayload {
  resourceMetrics: Array<{
    resource: {
      attributes: OTLPAttribute[];
    };
    scopeMetrics: Array<{
      scope?: {
        name: string;
        version: string;
      };
      metrics: OTLPMetric[];
    }>;
  }>;
}

// ============================================================================
// CONF
// ============================================================================

function getConfig() {
  return {
    instanceId: process.env.GRAFANA_INSTANCE_ID || "",
    apiKey: process.env.GRAFANA_API_KEY || "",
    endpoint:
      process.env.GRAFANA_OTLP_METRICS_ENDPOINT ||
      "https://otlp-gateway-prod-gb-south-1.grafana.net/otlp/v1/metrics",
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

async function sendMetrics(metrics: OTLPMetric[]): Promise<void> {
  try {
    const config = getConfig();

    if (!config.instanceId || !config.apiKey) {
      console.warn(
        "[GrafanaMetrics] Configuration manquante - GRAFANA_INSTANCE_ID ou GRAFANA_API_KEY non définis"
      );
      return;
    }

    const payload: OTLPMetricsPayload = {
      resourceMetrics: [
        {
          resource: {
            attributes: [
              { key: "service.name", value: { stringValue: config.serviceName } },
              { key: "service.version", value: { stringValue: config.serviceVersion } },
              { key: "environment", value: { stringValue: config.environment } },
              { key: "deployment.platform", value: { stringValue: "firebase-functions" } },
              {
                key: "cloud.region",
                value: { stringValue: process.env.FUNCTION_REGION || "europe-west1" },
              },
            ],
          },
          scopeMetrics: [
            {
              scope: {
                name: "uber-fight-functions-metrics",
                version: "1.0.0",
              },
              metrics,
            },
          ],
        },
      ],
    };

    const response = await fetch(config.endpoint, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errorText = await response.text().catch(() => "Unknown error");
      console.error(`[GrafanaMetrics] Échec envoi métriques: ${response.status} - ${errorText}`);
    }
  } catch (error) {
    console.error("[GrafanaMetrics] Erreur lors de l'envoi des métriques:", error);
  }
}

export async function pushCounter(
  name: string,
  value: number,
  attributes?: Record<string, LogAttributeValue>
): Promise<void> {
  const now = String(Date.now() * 1e6);

  const metric: OTLPMetric = {
    name,
    unit: "1",
    sum: {
      dataPoints: [
        {
          startTimeUnixNano: now,
          timeUnixNano: now,
          asInt: value,
          attributes: attributes ? attributesToOTLP(attributes) : undefined,
        },
      ],
      aggregationTemporality: 2,
      isMonotonic: true,
    },
  };

  await sendMetrics([metric]);
}

export async function pushGauge(
  name: string,
  value: number,
  attributes?: Record<string, LogAttributeValue>
): Promise<void> {
  const now = String(Date.now() * 1e6);

  const metric: OTLPMetric = {
    name,
    unit: "1",
    gauge: {
      dataPoints: [
        {
          startTimeUnixNano: now,
          timeUnixNano: now,
          asDouble: value,
          attributes: attributes ? attributesToOTLP(attributes) : undefined,
        },
      ],
    },
  };

  await sendMetrics([metric]);
}

export type { LogAttributeValue };
