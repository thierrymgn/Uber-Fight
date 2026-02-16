import { NextResponse } from 'next/server';
import { sendLog, sendBatchLogs } from '@/lib/grafana/logger';
import { logPerformance } from '@/lib/grafana/helpers';

export async function GET() {
  const results: Array<{ test: string; success: boolean; error?: string }> = [];

  try {
    await sendLog('Test log simple depuis /api/test-grafana');
    results.push({ test: 'Log simple', success: true });
  } catch (error) {
    results.push({
      test: 'Log simple',
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error',
    });
  }

  try {
    await sendLog('Test log niveau ERROR', 'error');
    await sendLog('Test log niveau WARN', 'warn');
    await sendLog('Test log niveau DEBUG', 'debug');
    results.push({ test: 'Log avec niveaux', success: true });
  } catch (error) {
    results.push({
      test: 'Log avec niveaux',
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error',
    });
  }

  try {
    await sendLog('Test avec contexte riche', 'info', {
      userId: 'test_user_123',
      action: 'test_action',
      testTimestamp: new Date().toISOString(),
      environment: 'test',
      numericValue: 42,
      booleanValue: true,
    });
    results.push({ test: 'Log avec contexte', success: true });
  } catch (error) {
    results.push({
      test: 'Log avec contexte',
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error',
    });
  }

  try {
    await logPerformance('database_query', 250, {
      query: 'SELECT * FROM users',
      cached: false,
    });
    results.push({ test: 'Performance Log', success: true });
  } catch (error) {
    results.push({
      test: 'Performance Log',
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error',
    });
  }

  try {
    await sendBatchLogs([
      { message: 'Batch log 1', level: 'info' },
      { message: 'Batch log 2', level: 'info', attributes: { index: 2 } },
      { message: 'Batch log 3', level: 'warn', attributes: { index: 3 } },
    ]);
    results.push({ test: 'Batch Logs', success: true });
  } catch (error) {
    results.push({
      test: 'Batch Logs',
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error',
    });
  }

  const successCount = results.filter((r) => r.success).length;
  const failCount = results.filter((r) => !r.success).length;

  return NextResponse.json({
    success: failCount === 0,
    message: `${successCount}/${results.length} tests réussis. Vérifie dans Grafana Cloud Explorer.`,
    timestamp: new Date().toISOString(),
    results,
    grafanaUrl: 'https://grafana.com/orgs/your-org/stacks/your-stack/logs',
    instructions: [
      '1. Connectez-vous à Grafana Cloud',
      '2. Allez dans Explore > Logs',
      "3. Sélectionnez la source 'grafanacloud-logs'",
      "4. Filtrez par service.name = 'uber-fight-backend'",
      '5. Vous devriez voir les logs de test',
    ],
  });
}
