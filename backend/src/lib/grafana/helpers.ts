import { sendLog, logError } from './logger';
import { pushCounter, pushHistogramValue } from './metrics';
import type { LogAttributeValue, FirebaseError } from './types';

export async function logFirebaseError(
  error: FirebaseError,
  operation: string,
  context?: Record<string, LogAttributeValue>
): Promise<void> {
  await logError(`Firebase Error: ${operation}`, {
    firebaseCode: error.code,
    errorMessage: error.message,
    operation,
    errorType: 'firebase',
    ...context,
  });
}

export async function logPerformance(
  operation: string,
  duration: number,
  metadata?: Record<string, LogAttributeValue>
): Promise<void> {
  let level: 'info' | 'warn' = 'info';
  if (duration > 5000) {
    level = 'warn';
  }

  await sendLog(`[Performance] ${operation}`, level, {
    operation,
    duration,
    durationUnit: 'ms',
    category: 'performance',
    slow: duration > 3000,
    ...metadata,
  });
}

export async function withPerformanceLogging<T>(
  operationName: string,
  operation: () => Promise<T>
): Promise<T> {
  const startTime = Date.now();

  try {
    const result = await operation();
    const duration = Date.now() - startTime;

    await logPerformance(operationName, duration, {
      status: 'success',
    });

    return result;
  } catch (error) {
    const duration = Date.now() - startTime;

    await logPerformance(operationName, duration, {
      status: 'error',
      errorMessage: error instanceof Error ? error.message : 'Unknown error',
    });

    throw error;
  }
}

export async function withApiMetrics<T extends Response>(
  routeName: string,
  method: string,
  handler: () => Promise<T>
): Promise<T> {
  const startTime = Date.now();

  try {
    const response = await handler();
    const duration = Date.now() - startTime;
    const status = response.status;
    const statusClass = `${Math.floor(status / 100)}xx`;

    pushCounter('http.server.request_count', 1, {
      'http.route': routeName,
      'http.method': method,
      'http.status_code': status,
      'http.status_class': statusClass,
    }).catch(() => {});

    pushHistogramValue('http.server.duration', duration, {
      'http.route': routeName,
      'http.method': method,
      'http.status_code': status,
    }).catch(() => {});

    if (status >= 500) {
      pushCounter('http.server.error_count', 1, {
        'http.route': routeName,
        'http.method': method,
        'http.status_code': status,
      }).catch(() => {});
    }

    return response;
  } catch (error) {
    const duration = Date.now() - startTime;

    pushCounter('http.server.error_count', 1, {
      'http.route': routeName,
      'http.method': method,
      'http.status_code': 500,
      'error.type': error instanceof Error ? error.constructor.name : 'Unknown',
    }).catch(() => {});

    pushHistogramValue('http.server.duration', duration, {
      'http.route': routeName,
      'http.method': method,
      'http.status_code': 500,
    }).catch(() => {});

    throw error;
  }
}
