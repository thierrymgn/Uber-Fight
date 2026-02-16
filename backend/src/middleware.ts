import { NextRequest, NextResponse } from 'next/server';
import { sendLog } from '@/lib/grafana/logger';

const EXCLUDED_PATTERNS = [
  /^\/_next\/static\//,
  /^\/_next\/image\//,
  /^\/favicon\.ico$/,
  /^\/logo\//,
  /^\/images\//,
  /^\/assets\//,
  /^\/public\//,
  /\.(png|jpg|jpeg|gif|svg|ico|webp|woff|woff2|ttf|eot|css|js|map)$/i,
];

function shouldExclude(pathname: string): boolean {
  return EXCLUDED_PATTERNS.some((pattern) => pattern.test(pathname));
}

function getClientIP(request: NextRequest): string {
  const forwardedFor = request.headers.get('x-forwarded-for');
  if (forwardedFor) {
    return forwardedFor.split(',')[0].trim();
  }

  const realIP = request.headers.get('x-real-ip');
  if (realIP) {
    return realIP;
  }

  const vercelForwardedFor = request.headers.get('x-vercel-forwarded-for');
  if (vercelForwardedFor) {
    return vercelForwardedFor;
  }

  return 'unknown';
}

export async function middleware(request: NextRequest) {
  const startTime = Date.now();
  const { pathname } = request.nextUrl;

  if (shouldExclude(pathname)) {
    return NextResponse.next();
  }

  const requestData = {
    method: request.method,
    url: request.url,
    path: pathname,
    ip: getClientIP(request),
    userAgent: request.headers.get('user-agent') || 'unknown',
    referer: request.headers.get('referer') || undefined,
  };

  const response = NextResponse.next();

  const duration = Date.now() - startTime;

  sendLog(`[Middleware] ${requestData.method} ${requestData.path}`, 'info', {
    ...requestData,
    duration,
    category: 'http_middleware',
    phase: 'request',
  }).catch((error) => {
    console.error('[Middleware] Failed to send log:', error);
  });

  return response;
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};
