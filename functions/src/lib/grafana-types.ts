export type LogLevel = "info" | "warn" | "error" | "debug";

export type OTLPSeverity = "INFO" | "WARN" | "ERROR" | "DEBUG";

export const SEVERITY_NUMBER: Record<LogLevel, number> = {
  debug: 5,
  info: 9,
  warn: 13,
  error: 17,
};

export type LogAttributeValue = string | number | boolean | null | undefined;

export interface GrafanaConfig {
  instanceId: string;
  apiKey: string;
  endpoint: string;
  serviceName: string;
  serviceVersion: string;
  environment: string;
}

export interface OTLPAttribute {
  key: string;
  value: OTLPAttributeValue;
}

export interface OTLPAttributeValue {
  stringValue?: string;
  intValue?: number;
  boolValue?: boolean;
}

export interface OTLPLogRecord {
  timeUnixNano: string;
  severityText: OTLPSeverity;
  severityNumber: number;
  body: { stringValue: string };
  attributes?: OTLPAttribute[];
}

export interface OTLPLogsPayload {
  resourceLogs: Array<{
    resource: {
      attributes: OTLPAttribute[];
    };
    scopeLogs: Array<{
      scope?: {
        name: string;
        version: string;
      };
      logRecords: OTLPLogRecord[];
    }>;
  }>;
}
