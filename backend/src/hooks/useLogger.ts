'use client';

import { useCallback } from 'react';

type LogLevel = 'info' | 'warn' | 'error' | 'debug';

interface LogAttributes {
  [key: string]: string | number | boolean | null | undefined;
}

interface LogFunction {
  (message: string, attributes?: LogAttributes): void;
}

interface UseLoggerReturn {
  logInfo: LogFunction;
  logWarn: LogFunction;
  logError: LogFunction;
  logDebug: LogFunction;
  log: (message: string, level?: LogLevel, attributes?: LogAttributes) => void;
}

const LOG_API_URL = '/api/logs';

export function useLogger(): UseLoggerReturn {
  const sendToAPI = useCallback(
    async (message: string, level: LogLevel, attributes?: LogAttributes): Promise<void> => {
      try {
        const response = await fetch(LOG_API_URL, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            level,
            message,
            attributes: {
              ...attributes,
              clientTimestamp: new Date().toISOString(),
              pageUrl: typeof window !== 'undefined' ? window.location.href : undefined,
              pageTitle: typeof document !== 'undefined' ? document.title : undefined,
            },
          }),
        });

        if (!response.ok) {
          console.warn(`[useLogger] Failed to send log: ${response.status}`);
        }
      } catch (error) {
        console.error('[useLogger] Error sending log:', error);
      }
    },
    []
  );

  const log = useCallback(
    (message: string, level: LogLevel = 'info', attributes?: LogAttributes): void => {
      sendToAPI(message, level, attributes);

      if (process.env.NODE_ENV === 'development') {
        const consoleFn =
          level === 'error' ? console.error : level === 'warn' ? console.warn : console.log;
        consoleFn(`[${level.toUpperCase()}] ${message}`, attributes || '');
      }
    },
    [sendToAPI]
  );

  const logInfo: LogFunction = useCallback(
    (message: string, attributes?: LogAttributes) => {
      log(message, 'info', attributes);
    },
    [log]
  );

  const logWarn: LogFunction = useCallback(
    (message: string, attributes?: LogAttributes) => {
      log(message, 'warn', attributes);
    },
    [log]
  );

  const logError: LogFunction = useCallback(
    (message: string, attributes?: LogAttributes) => {
      log(message, 'error', attributes);
    },
    [log]
  );

  const logDebug: LogFunction = useCallback(
    (message: string, attributes?: LogAttributes) => {
      log(message, 'debug', attributes);
    },
    [log]
  );

  return {
    logInfo,
    logWarn,
    logError,
    logDebug,
    log,
  };
}

export async function clientLog(
  message: string,
  level: LogLevel = 'info',
  attributes?: LogAttributes
): Promise<void> {
  try {
    const response = await fetch(LOG_API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        level,
        message,
        attributes: {
          ...attributes,
          clientTimestamp: new Date().toISOString(),
          pageUrl: typeof window !== 'undefined' ? window.location.href : undefined,
        },
      }),
    });

    if (!response.ok && process.env.NODE_ENV === 'development') {
      console.warn(`[clientLog] Failed to send log: ${response.status}`);
    }
  } catch (error) {
    if (process.env.NODE_ENV === 'development') {
      console.error('[clientLog] Error sending log:', error);
    }
  }
}

export default useLogger;
