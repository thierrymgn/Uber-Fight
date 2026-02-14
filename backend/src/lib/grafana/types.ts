export type LogLevel = "info" | "warn" | "error" | "debug";

export type OTLPSeverity = "INFO" | "WARN" | "ERROR" | "DEBUG";

export const SEVERITY_NUMBER: Record<LogLevel, number> = {
  debug: 5,
  info: 9,
  warn: 13,
  error: 17,
};

export interface LogEntry {
  message: string;
  level?: LogLevel;
  attributes?: Record<string, LogAttributeValue>;
  timestamp?: string;
}

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

export interface OTLPScopeLog {
  scope?: {
    name: string;
    version: string;
  };
  logRecords: OTLPLogRecord[];
}

export interface OTLPResourceLog {
  resource: {
    attributes: OTLPAttribute[];
  };
  scopeLogs: OTLPScopeLog[];
}

export interface OTLPLogsPayload {
  resourceLogs: OTLPResourceLog[];
}

export interface FirebaseError extends Error {
  code: string;
  customData?: Record<string, unknown>;
}
