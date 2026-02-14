import { NextRequest, NextResponse } from "next/server";
import { sendLog } from "@/lib/grafana/logger";
import type { LogLevel, LogAttributeValue } from "@/lib/grafana/types";

interface LogRequestBody {
  level: LogLevel;
  message: string;
  attributes?: Record<string, LogAttributeValue>;
}

const ALLOWED_LEVELS: LogLevel[] = ["info", "warn", "error", "debug"];

function validateRequestBody(body: unknown): LogRequestBody | null {
  if (!body || typeof body !== "object") {
    return null;
  }

  const data = body as Record<string, unknown>;

  if (typeof data.message !== "string" || data.message.trim().length === 0) {
    return null;
  }

  const level = (data.level as LogLevel) || "info";
  if (!ALLOWED_LEVELS.includes(level)) {
    return null;
  }

  let attributes: Record<string, LogAttributeValue> | undefined;
  if (data.attributes !== undefined) {
    if (typeof data.attributes !== "object" || Array.isArray(data.attributes)) {
      return null;
    }
    attributes = data.attributes as Record<string, LogAttributeValue>;
  }

  return {
    level,
    message: data.message.substring(0, 2000),
    attributes,
  };
}

const rateLimitStore = new Map<string, { count: number; resetTime: number }>();

const RATE_LIMIT = {
  maxRequests: 100,
  windowMs: 60 * 1000,
};

function checkRateLimit(ip: string): { allowed: boolean; remaining: number } {
  const now = Date.now();
  const entry = rateLimitStore.get(ip);

  if (rateLimitStore.size > 10000) {
    const cutoff = now - RATE_LIMIT.windowMs;
    for (const [key, value] of rateLimitStore.entries()) {
      if (value.resetTime < cutoff) {
        rateLimitStore.delete(key);
      }
    }
  }

  if (!entry || now > entry.resetTime) {
    rateLimitStore.set(ip, {
      count: 1,
      resetTime: now + RATE_LIMIT.windowMs,
    });
    return { allowed: true, remaining: RATE_LIMIT.maxRequests - 1 };
  }

  if (entry.count >= RATE_LIMIT.maxRequests) {
    return { allowed: false, remaining: 0 };
  }

  entry.count++;
  return { allowed: true, remaining: RATE_LIMIT.maxRequests - entry.count };
}

const SENSITIVE_KEYS = [
  "password",
  "token",
  "secret",
  "apikey",
  "api_key",
  "authorization",
  "auth",
  "credit_card",
  "creditcard",
  "card_number",
  "cardnumber",
  "cvv",
  "cvc",
  "ssn",
  "social_security",
  "private_key",
  "privatekey",
  "access_token",
  "refresh_token",
];

function sanitizeAttributes(
  attributes: Record<string, LogAttributeValue> | undefined
): Record<string, LogAttributeValue> | undefined {
  if (!attributes) return undefined;

  const sanitized: Record<string, LogAttributeValue> = {};

  for (const [key, value] of Object.entries(attributes)) {
    const lowerKey = key.toLowerCase().replace(/[-_]/g, "");

    const isSensitive = SENSITIVE_KEYS.some((sensitive) =>
      lowerKey.includes(sensitive.replace(/[-_]/g, ""))
    );

    if (isSensitive) {
      sanitized[key] = "[REDACTED]";
    } else if (typeof value === "string" && value.length > 500) {
      sanitized[key] = value.substring(0, 500) + "...[truncated]";
    } else {
      sanitized[key] = value;
    }
  }

  return sanitized;
}

function getClientIP(request: NextRequest): string {
  const forwardedFor = request.headers.get("x-forwarded-for");
  if (forwardedFor) {
    return forwardedFor.split(",")[0].trim();
  }

  const realIP = request.headers.get("x-real-ip");
  if (realIP) {
    return realIP;
  }

  const vercelForwardedFor = request.headers.get("x-vercel-forwarded-for");
  if (vercelForwardedFor) {
    return vercelForwardedFor;
  }

  return "unknown";
}

// ============================================================================
// ROUTE HANDLER
// ============================================================================

export async function POST(request: NextRequest) {
  try {
    const clientIP = getClientIP(request);

    const rateLimit = checkRateLimit(clientIP);
    if (!rateLimit.allowed) {
      return NextResponse.json(
        {
          success: false,
          error: "Rate limit exceeded. Maximum 100 logs per minute.",
        },
        {
          status: 429,
          headers: {
            "X-RateLimit-Limit": String(RATE_LIMIT.maxRequests),
            "X-RateLimit-Remaining": "0",
            "Retry-After": "60",
          },
        }
      );
    }

    let body: unknown;
    try {
      body = await request.json();
    } catch {
      return NextResponse.json(
        { success: false, error: "Invalid JSON body" },
        { status: 400 }
      );
    }

    const validatedBody = validateRequestBody(body);
    if (!validatedBody) {
      return NextResponse.json(
        {
          success: false,
          error:
            'Invalid request body. Expected: { level: "info"|"warn"|"error"|"debug", message: string, attributes?: object }',
        },
        { status: 400 }
      );
    }

    const sanitizedAttributes = sanitizeAttributes(validatedBody.attributes);

    const enrichedAttributes: Record<string, LogAttributeValue> = {
      ...sanitizedAttributes,
      source: "client",
      clientIP,
      userAgent: request.headers.get("user-agent") || "unknown",
      timestamp: new Date().toISOString(),
    };

    sendLog(validatedBody.message, validatedBody.level, enrichedAttributes).catch(
      (error) => {
        console.error("[API/logs] Failed to send log to Grafana:", error);
      }
    );

    return NextResponse.json(
      { success: true, message: "Log received" },
      {
        status: 200,
        headers: {
          "X-RateLimit-Limit": String(RATE_LIMIT.maxRequests),
          "X-RateLimit-Remaining": String(rateLimit.remaining),
        },
      }
    );
  } catch (error) {
    console.error("[API/logs] Unexpected error:", error);

    return NextResponse.json(
      { success: false, error: "Internal server error" },
      { status: 500 }
    );
  }
}
