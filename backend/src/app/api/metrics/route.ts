import { NextRequest, NextResponse } from 'next/server';
import { pushCounter, pushGauge, pushHistogramValue } from '@/lib/grafana/metrics';
import type { LogAttributeValue } from '@/lib/grafana/types';

type MetricType = 'counter' | 'gauge' | 'histogram';

interface MetricRequestBody {
  type: MetricType;
  name: string;
  value: number;
  attributes?: Record<string, LogAttributeValue>;
}

const ALLOWED_TYPES: MetricType[] = ['counter', 'gauge', 'histogram'];

const ALLOWED_METRIC_PREFIXES = [
  'mobile.app.',
  'mobile.screen.',
  'mobile.network.',
  'mobile.fight.',
  'mobile.auth.',
  'mobile.user.',
];

function validateRequestBody(body: unknown): MetricRequestBody | null {
  if (!body || typeof body !== 'object') return null;

  const data = body as Record<string, unknown>;

  const type = data.type as MetricType;
  if (!type || !ALLOWED_TYPES.includes(type)) return null;

  if (typeof data.name !== 'string' || data.name.trim().length === 0) return null;
  if (!ALLOWED_METRIC_PREFIXES.some((prefix) => (data.name as string).startsWith(prefix)))
    return null;

  if (typeof data.value !== 'number' || !isFinite(data.value)) return null;

  let attributes: Record<string, LogAttributeValue> | undefined;
  if (data.attributes !== undefined) {
    if (typeof data.attributes !== 'object' || Array.isArray(data.attributes)) return null;
    attributes = data.attributes as Record<string, LogAttributeValue>;
  }

  return {
    type,
    name: data.name.substring(0, 200),
    value: data.value,
    attributes,
  };
}

const rateLimitStore = new Map<string, { count: number; resetTime: number }>();

const RATE_LIMIT = {
  maxRequests: 200,
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

function getClientIP(request: NextRequest): string {
  const forwardedFor = request.headers.get('x-forwarded-for');
  if (forwardedFor) return forwardedFor.split(',')[0].trim();

  const realIP = request.headers.get('x-real-ip');
  if (realIP) return realIP;

  const vercelForwardedFor = request.headers.get('x-vercel-forwarded-for');
  if (vercelForwardedFor) return vercelForwardedFor;

  return 'unknown';
}

export async function POST(request: NextRequest) {
  try {
    const clientIP = getClientIP(request);

    const rateLimit = checkRateLimit(clientIP);
    if (!rateLimit.allowed) {
      return NextResponse.json(
        { success: false, error: 'Rate limit exceeded. Maximum 200 metrics per minute.' },
        {
          status: 429,
          headers: {
            'X-RateLimit-Limit': String(RATE_LIMIT.maxRequests),
            'X-RateLimit-Remaining': '0',
            'Retry-After': '60',
          },
        }
      );
    }

    let body: unknown;
    try {
      body = await request.json();
    } catch {
      return NextResponse.json({ success: false, error: 'Invalid JSON body' }, { status: 400 });
    }

    const validated = validateRequestBody(body);
    if (!validated) {
      return NextResponse.json(
        {
          success: false,
          error:
            'Invalid body. Expected: { type: "counter"|"gauge"|"histogram", name: "mobile.*", value: number, attributes?: object }',
        },
        { status: 400 }
      );
    }

    const enrichedAttributes: Record<string, LogAttributeValue> = {
      ...validated.attributes,
      source: 'mobile-android',
    };

    switch (validated.type) {
      case 'counter':
        pushCounter(validated.name, validated.value, enrichedAttributes).catch(() => {});
        break;
      case 'gauge':
        pushGauge(validated.name, validated.value, enrichedAttributes).catch(() => {});
        break;
      case 'histogram':
        pushHistogramValue(validated.name, validated.value, enrichedAttributes).catch(() => {});
        break;
    }

    return NextResponse.json(
      { success: true },
      {
        status: 200,
        headers: {
          'X-RateLimit-Limit': String(RATE_LIMIT.maxRequests),
          'X-RateLimit-Remaining': String(rateLimit.remaining),
        },
      }
    );
  } catch (error) {
    console.error('[API/metrics] Unexpected error:', error);
    return NextResponse.json({ success: false, error: 'Internal server error' }, { status: 500 });
  }
}
