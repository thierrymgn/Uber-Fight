'use client';

import { ExternalLink, AlertCircle } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';

interface GrafanaEmbedProps {
  dashboardUrl?: string;
  title?: string;
  description?: string;
}

export function GrafanaEmbed({
  dashboardUrl,
  title = 'Monitoring Grafana',
  description = 'Logs et métriques en temps réel',
}: GrafanaEmbedProps) {
  if (!dashboardUrl) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{title}</CardTitle>
          <CardDescription>{description}</CardDescription>
        </CardHeader>
        <CardContent>
          <Alert>
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>Configuration requise</AlertTitle>
            <AlertDescription className="space-y-2">
              <p>Le dashboard Grafana n&apos;est pas configuré. Pour l&apos;activer :</p>
              <ol className="list-decimal list-inside space-y-1 text-sm">
                <li>Connectez-vous à votre instance Grafana Cloud</li>
                <li>Créez un dashboard ou utilisez-en un existant</li>
                <li>Cliquez sur &quot;Share&quot; puis copiez le lien de partage</li>
                <li>
                  Ajoutez la variable d&apos;environnement{' '}
                  <code className="bg-muted px-1 py-0.5 rounded">
                    NEXT_PUBLIC_GRAFANA_DASHBOARD_URL
                  </code>
                </li>
              </ol>
              <Button
                variant="outline"
                size="sm"
                className="mt-2"
                onClick={() => window.open('https://grafana.com/docs/grafana-cloud/', '_blank')}
              >
                <ExternalLink className="h-4 w-4 mr-2" />
                Documentation Grafana
              </Button>
            </AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <p className="text-sm text-muted-foreground">
          Cliquez sur le bouton ci-dessous pour accéder au dashboard Grafana et visualiser les logs,
          métriques et performances en temps réel.
        </p>

        <Button
          size="lg"
          className="w-full"
          onClick={() => window.open(dashboardUrl, '_blank', 'noopener,noreferrer')}
        >
          <ExternalLink className="mr-2 h-5 w-5" />
          Ouvrir Grafana Dashboard
        </Button>

        <div className="text-xs text-muted-foreground text-center">
          Le dashboard s&apos;ouvrira dans un nouvel onglet
        </div>
      </CardContent>
    </Card>
  );
}

export default GrafanaEmbed;
