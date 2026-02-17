import type { OTLPAttribute, OTLPAttributeValue } from './types';

export type AggregationTemporality = 1 | 2;

export interface OTLPNumberDataPoint {
  attributes?: OTLPAttribute[];
  startTimeUnixNano: string;
  timeUnixNano: string;
  asInt?: number;
  asDouble?: number;
}

export interface OTLPHistogramDataPoint {
  attributes?: OTLPAttribute[];
  startTimeUnixNano: string;
  timeUnixNano: string;
  count: number;
  sum: number;
  bucketCounts: number[];
  explicitBounds: number[];
}

export interface OTLPSum {
  dataPoints: OTLPNumberDataPoint[];
  aggregationTemporality: AggregationTemporality;
  isMonotonic: boolean;
}

export interface OTLPGauge {
  dataPoints: OTLPNumberDataPoint[];
}

export interface OTLPHistogram {
  dataPoints: OTLPHistogramDataPoint[];
  aggregationTemporality: AggregationTemporality;
}

export interface OTLPMetric {
  name: string;
  description?: string;
  unit?: string;
  sum?: OTLPSum;
  gauge?: OTLPGauge;
  histogram?: OTLPHistogram;
}

export interface OTLPScopeMetrics {
  scope?: {
    name: string;
    version: string;
  };
  metrics: OTLPMetric[];
}

export interface OTLPResourceMetrics {
  resource: {
    attributes: OTLPAttribute[];
  };
  scopeMetrics: OTLPScopeMetrics[];
}

export interface OTLPMetricsPayload {
  resourceMetrics: OTLPResourceMetrics[];
}

export type { OTLPAttribute, OTLPAttributeValue };
