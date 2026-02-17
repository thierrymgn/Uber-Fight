import type { LogAttributeValue, OTLPAttribute } from './types';
import type {
  OTLPMetric,
  OTLPMetricsPayload,
} from './metric-types';

// ============================================================================
// CONF
// ============================================================================

function getConfig() {
  return {
    instanceId: process.env.GRAFANA_INSTANCE_ID || '',
    apiKey: process.env.GRAFANA_API_KEY || '',
    endpoint:
      process.env.GRAFANA_OTLP_METRICS_ENDPOINT ||
      'https://otlp-gateway-prod-gb-south-1.grafana.net/otlp/v1/metrics',
    serviceName: 'uber-fight-backend',
    serviceVersion: '1.0.0',
    environment: process.env.NEXT_PUBLIC_APP_ENV || process.env.NODE_ENV || 'development',
  };
}

function getAuthHeaders(): Record<string, string> {
  const config = getConfig();
  const authPair = `${config.instanceId}:${config.apiKey}`;
  const encoded = Buffer.from(authPair).toString('base64');

  return {
    'Content-Type': 'application/json',
    Authorization: `Basic ${encoded}`,
  };
}

// ============================================================================
// UTILS
// ============================================================================

function toOTLPAttribute(key: string, value: LogAttributeValue): OTLPAttribute {
  if (value === null || value === undefined) {
    return { key, value: { stringValue: '' } };
  }

  if (typeof value === 'boolean') {
    return { key, value: { boolValue: value } };
  }

  if (typeof value === 'number') {
    if (Number.isInteger(value)) {
      return { key, value: { intValue: value } };
    }
    return { key, value: { stringValue: String(value) } };
  }

  return { key, value: { stringValue: String(value) } };
}

function attributesToOTLP(attributes: Record<string, LogAttributeValue>): OTLPAttribute[] {
  return Object.entries(attributes).map(([key, value]) => toOTLPAttribute(key, value));
}

function createMetricsPayload(metrics: OTLPMetric[]): OTLPMetricsPayload {
  const config = getConfig();

  return {
    resourceMetrics: [
      {
        resource: {
          attributes: [
            { key: 'service.name', value: { stringValue: config.serviceName } },
            { key: 'service.version', value: { stringValue: config.serviceVersion } },
            { key: 'environment', value: { stringValue: config.environment } },
            { key: 'deployment.platform', value: { stringValue: 'vercel' } },
          ],
        },
        scopeMetrics: [
          {
            scope: {
              name: 'uber-fight-metrics',
              version: '1.0.0',
            },
            metrics,
          },
        ],
      },
    ],
  };
}

// ============================================================================
// SEND
// ============================================================================

async function sendMetrics(metrics: OTLPMetric[]): Promise<void> {
  try {
    const config = getConfig();

    if (!config.instanceId || !config.apiKey) {
      console.warn(
        '[GrafanaMetrics] Configuration manquante - GRAFANA_INSTANCE_ID ou GRAFANA_API_KEY non définis'
      );
      return;
    }

    const payload = createMetricsPayload(metrics);

    const response = await fetch(config.endpoint, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errorText = await response.text().catch(() => 'Unknown error');
      console.error(`[GrafanaMetrics] Échec envoi métriques: ${response.status} - ${errorText}`);
    }
  } catch (error) {
    console.error("[GrafanaMetrics] Erreur lors de l'envoi des métriques:", error);
  }
}

// ============================================================================
// PUBLIC API
// ============================================================================

export async function pushCounter(
  name: string,
  value: number,
  attributes?: Record<string, LogAttributeValue>
): Promise<void> {
  const now = String(Date.now() * 1e6);

  const metric: OTLPMetric = {
    name,
    unit: '1',
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
    unit: '1',
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

const DEFAULT_HISTOGRAM_BOUNDS = [5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000];

export async function pushHistogramValue(
  name: string,
  value: number,
  attributes?: Record<string, LogAttributeValue>,
  bounds?: number[]
): Promise<void> {
  const effectiveBounds = bounds || DEFAULT_HISTOGRAM_BOUNDS;
  const bucketCounts = new Array(effectiveBounds.length + 1).fill(0);

  let placed = false;
  for (let i = 0; i < effectiveBounds.length; i++) {
    if (value <= effectiveBounds[i]) {
      bucketCounts[i] = 1;
      placed = true;
      break;
    }
  }
  if (!placed) {
    bucketCounts[effectiveBounds.length] = 1;
  }

  const now = String(Date.now() * 1e6);

  const metric: OTLPMetric = {
    name,
    unit: 'ms',
    histogram: {
      dataPoints: [
        {
          startTimeUnixNano: now,
          timeUnixNano: now,
          count: 1,
          sum: value,
          bucketCounts,
          explicitBounds: effectiveBounds,
          attributes: attributes ? attributesToOTLP(attributes) : undefined,
        },
      ],
      aggregationTemporality: 2,
    },
  };

  await sendMetrics([metric]);
}
