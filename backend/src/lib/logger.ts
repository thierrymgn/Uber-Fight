const INSTANCE_ID = process.env.GRAFANA_INSTANCE_ID || "votre_instance_id";
const API_KEY = process.env.GRAFANA_API_KEY || "votre_api_key";
const authPair = `${INSTANCE_ID}:${API_KEY}`;
const encoded = Buffer.from(authPair).toString('base64');

export async function sendLog(message: string, level: 'info' | 'error' | 'warn' = 'info') {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Basic ${encoded}`,
  };

  const body = {
    resourceLogs: [{
      resource: {
        attributes: [
          { key: "service.name", value: { stringValue: "my-nextjs-app" } },
          { key: "environment", value: { stringValue: process.env.NODE_ENV } }
        ]
      },
      scopeLogs: [{
        logRecords: [{
          timeUnixNano: String(Date.now() * 1e6),
          severityText: level.toUpperCase(),
          body: { stringValue: message }
        }]
      }]
    }]
  };

  const url = process.env.GRAFANA_OTLP_ENDPOINT || "https://otlp-gateway-prod-gb-south-1.grafana.net/otlp/v1/logs";
  
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(body)
    });
    
    if (!response.ok) {
      console.error("Failed to send log:", response.status);
    }
  } catch (error) {
    console.error("Error sending log:", error);
  }
}