'use client';

/**
 * Error Boundary React to capture unhandled errors.
 *
 * @module components/ErrorBoundary
 * @description
 *
 * @example
 * <ErrorBoundary fallback={<ErrorPage />}>
 *   <MyComponent />
 * </ErrorBoundary>
 */

import { Component, ErrorInfo, ReactNode } from 'react';
import { clientLog } from '@/hooks/useLogger';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { AlertCircle, RefreshCw, RotateCcw, ChevronDown } from 'lucide-react';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
    };
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    this.setState({ errorInfo });

    this.logError(error, errorInfo);

    if (this.props.onError) {
      this.props.onError(error, errorInfo);
    }
  }

  private async logError(error: Error, errorInfo: ErrorInfo): Promise<void> {
    try {
      await clientLog(`[ErrorBoundary] ${error.name}: ${error.message}`, 'error', {
        errorName: error.name,
        errorMessage: error.message,
        stack: error.stack?.substring(0, 1000),
        componentStack: errorInfo.componentStack?.substring(0, 1000),
        route: typeof window !== 'undefined' ? window.location.pathname : undefined,
        url: typeof window !== 'undefined' ? window.location.href : undefined,
        userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : undefined,
        timestamp: new Date().toISOString(),
        category: 'react_error_boundary',
      });
    } catch (logError) {
      console.error('[ErrorBoundary] Failed to send error log:', logError);
    }
  }

  private handleReset = (): void => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
    });
  };

  private handleReload = (): void => {
    if (typeof window !== 'undefined') {
      window.location.reload();
    }
  };

  render(): ReactNode {
    const { hasError, error } = this.state;
    const { children, fallback } = this.props;

    if (hasError) {
      if (fallback) {
        return fallback;
      }

      return (
        <div className="min-h-screen flex items-center justify-center p-4 bg-linear-to-b from-background to-muted/20">
          <Card className="w-full max-w-md shadow-lg">
            <CardHeader className="text-center pb-2">
              <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-destructive/10">
                <AlertCircle className="h-8 w-8 text-destructive" />
              </div>
              <CardTitle className="text-2xl">Oups ! Une erreur est survenue</CardTitle>
              <CardDescription className="text-base mt-2">
                Nous avons rencontré un problème inattendu. Notre équipe a été notifiée et travaille
                à le résoudre.
              </CardDescription>
            </CardHeader>

            <CardContent className="space-y-4">
              {process.env.NODE_ENV === 'development' && error && (
                <details className="group rounded-lg border bg-muted/50 p-3">
                  <summary className="flex cursor-pointer items-center justify-between text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
                    <span>Détails de l&apos;erreur (dev only)</span>
                    <ChevronDown className="h-4 w-4 transition-transform group-open:rotate-180" />
                  </summary>
                  <pre className="mt-3 overflow-x-auto rounded-md bg-destructive/5 p-3 text-xs text-destructive whitespace-pre-wrap wrap-break-word">
                    <strong>{error.name}:</strong> {error.message}
                    {'\n\n'}
                    {error.stack}
                  </pre>
                </details>
              )}
            </CardContent>

            <CardFooter className="flex flex-col sm:flex-row gap-3 pt-2">
              <Button variant="outline" className="w-full sm:w-auto" onClick={this.handleReset}>
                <RotateCcw className="mr-2 h-4 w-4" />
                Réessayer
              </Button>
              <Button className="w-full sm:w-auto" onClick={this.handleReload}>
                <RefreshCw className="mr-2 h-4 w-4" />
                Recharger la page
              </Button>
            </CardFooter>
          </Card>
        </div>
      );
    }

    return children;
  }
}

export default ErrorBoundary;
