import { sendLog, logError } from "./logger";
import type { LogAttributeValue, FirebaseError } from "./types";

export async function logFirebaseError(
  error: FirebaseError,
  operation: string,
  context?: Record<string, LogAttributeValue>,
): Promise<void> {
  await logError(`Firebase Error: ${operation}`, {
    firebaseCode: error.code,
    errorMessage: error.message,
    operation,
    errorType: "firebase",
    ...context,
  });
}

export async function logPerformance(
  operation: string,
  duration: number,
  metadata?: Record<string, LogAttributeValue>,
): Promise<void> {
  let level: "info" | "warn" = "info";
  if (duration > 5000) {
    level = "warn";
  }

  await sendLog(`[Performance] ${operation}`, level, {
    operation,
    duration,
    durationUnit: "ms",
    category: "performance",
    slow: duration > 3000,
    ...metadata,
  });
}

export async function withPerformanceLogging<T>(
  operationName: string,
  operation: () => Promise<T>,
): Promise<T> {
  const startTime = Date.now();

  try {
    const result = await operation();
    const duration = Date.now() - startTime;

    await logPerformance(operationName, duration, {
      status: "success",
    });

    return result;
  } catch (error) {
    const duration = Date.now() - startTime;

    await logPerformance(operationName, duration, {
      status: "error",
      errorMessage: error instanceof Error ? error.message : "Unknown error",
    });

    throw error;
  }
}
