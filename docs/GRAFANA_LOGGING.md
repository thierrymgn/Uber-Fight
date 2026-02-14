# 📊 Guide de Logging Grafana Cloud - Uber-Fight

Ce document décrit l'architecture et l'utilisation du système de logging centralisé vers Grafana Cloud pour le projet Uber-Fight.

## 📑 Table des matières

1. [Architecture Overview](#architecture-overview)
2. [Backend Next.js - Exemples](#backend-nextjs---exemples)
3. [Firebase Functions - Exemples](#firebase-functions---exemples)
4. [Mobile Android - Exemples](#mobile-android---exemples)
5. [Querying Logs in Grafana](#querying-logs-in-grafana)
6. [Troubleshooting](#troubleshooting)
7. [Best Practices](#best-practices)

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        GRAFANA CLOUD                             │
│                    (OTLP Logs Endpoint)                          │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ HTTPS (Basic Auth)
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐   ┌─────────────────┐   ┌─────────────────┐
│  Backend      │   │   Firebase      │   │    Mobile       │
│  Next.js      │   │   Functions     │   │    Android      │
│  (Vercel)     │   │   (GCP)         │   │    (Kotlin)     │
└───────────────┘   └─────────────────┘   └────────┬────────┘
        │                                          │
        │                                          │
        │◄─────────────────────────────────────────┘
        │        POST /api/logs
        │
┌───────────────────────────────────────────────────────────────┐
│                    CLIENT SIDE                                 │
│  (React Components, Browser, Mobile App)                       │
│  → Tous les logs passent par /api/logs                        │
│  → JAMAIS d'accès direct à Grafana                            │
└───────────────────────────────────────────────────────────────┘
```

### Points clés

- **Backend & Functions** → Accès direct à Grafana Cloud via OTLP
- **Client (React/Mobile)** → Passe par `/api/logs` (sécurité)
- **Protocole** → OpenTelemetry (OTLP) sur HTTPS
- **Authentification** → Basic Auth avec Instance ID + API Key

---

## 🌐 Backend Next.js - Exemples

### Configuration des variables d'environnement

```bash
# backend/.env.local
GRAFANA_INSTANCE_ID="votre_instance_id"
GRAFANA_API_KEY="glc_xxx"
GRAFANA_OTLP_ENDPOINT="https://otlp-gateway-prod-gb-south-1.grafana.net/otlp/v1/logs"
NEXT_PUBLIC_APP_ENV=development
```

### Logger côté serveur

```typescript
// Importer depuis le module grafana
import { sendLog, logInfo, logError, logWarn } from "@/lib/grafana";

// Log simple
await sendLog("User logged in");

// Log avec niveau
await sendLog("Payment processing started", "info");
await sendLog("Low disk space", "warn");
await sendLog("Database connection failed", "error");

// Log avec contexte
await sendLog("Fight created", "info", {
  fightId: "fight_123",
  userId: "user_abc",
  location: "Paris",
  amount: 49.99,
});

// Raccourcis
await logInfo("Operation completed");
await logWarn("Deprecated API used");
await logError("Critical failure");
```

### Helpers métier

```typescript
import {
  logPerformance,
  logFirebaseError,
  withPerformanceLogging,
} from "@/lib/grafana";

// Performance
await logPerformance("database_query", 250, {
  query: "SELECT * FROM users",
});

// Erreur Firebase
try {
  await signIn(email, password);
} catch (error) {
  await logFirebaseError(error, "user_login", { email });
}

// Wrapper performance auto
const result = await withPerformanceLogging("fetchUsers", async () => {
  return await db.collection("users").get();
});
```

### Batch de logs

```typescript
import { sendBatchLogs } from "@/lib/grafana";

// Envoyer plusieurs logs en une seule requête
await sendBatchLogs([
  { message: "Step 1 completed", level: "info" },
  { message: "Step 2 completed", level: "info" },
  { message: "All steps done", level: "info", attributes: { totalSteps: 3 } },
]);
```

### Hook React (côté client)

```tsx
"use client";

import { useLogger } from "@/hooks/useLogger";

function MyComponent() {
  const { logInfo, logError, logWarn } = useLogger();

  const handleClick = () => {
    logInfo("Button clicked", { buttonId: "cta-signup" });
  };

  const handleSubmit = async (data: FormData) => {
    try {
      await submitForm(data);
      logInfo("Form submitted successfully");
    } catch (error) {
      logError("Form submission failed", { error: error.message });
    }
  };

  return <button onClick={handleClick}>Sign Up</button>;
}
```

### Error Boundary

```tsx
import { ErrorBoundary } from "@/components/ErrorBoundary";

// Dans un layout ou page
function MyLayout({ children }) {
  return (
    <ErrorBoundary
      fallback={<div>Une erreur est survenue</div>}
      onError={(error, errorInfo) => {
        // Callback optionnel
        console.log("Error caught:", error);
      }}
    >
      {children}
    </ErrorBoundary>
  );
}
```

### Middleware (auto-logging)

Le middleware `backend/middleware.ts` log automatiquement toutes les requêtes HTTP (sauf assets statiques).

---

## ⚡ Firebase Functions - Exemples

### Configuration

```bash
# functions/.env
GRAFANA_INSTANCE_ID=votre_instance_id
GRAFANA_API_KEY=glc_xxx
GRAFANA_OTLP_ENDPOINT=https://otlp-gateway-prod-gb-south-1.grafana.net/otlp/v1/logs
```

### Logger

```typescript
import { logFunction } from "./lib/grafana-logger";

export const myFunction = functions.https.onCall(async (data, context) => {
  // Log de début
  await logFunction("myFunction", "Function started", "info", {
    uid: context.auth?.uid,
  });

  try {
    // Votre logique
    const result = await doSomething(data);

    // Log de succès
    await logFunction("myFunction", "Function completed", "info", {
      resultId: result.id,
    });

    return result;
  } catch (error) {
    // Log d'erreur
    await logFunction("myFunction", "Function failed", "error", {
      error: error.message,
    });
    throw error;
  }
});
```

---

## 📱 Mobile Android - Exemples

### Logger Kotlin

```kotlin
import com.example.mobile_uber_fight.logger.GrafanaLogger

// Log simple
GrafanaLogger.logInfo("User opened app")

// Log avec attributs
GrafanaLogger.logInfo("Fight viewed", mapOf(
    "fightId" to "fight_123",
    "userId" to "user_abc"
))

// Log d'erreur
try {
    api.fetchData()
} catch (e: Exception) {
    GrafanaLogger.logError("API call failed", e, mapOf(
        "endpoint" to "/api/data"
    ))
}

// Log d'action utilisateur
GrafanaLogger.logUserAction("button_clicked", mapOf(
    "buttonId" to "book_fight",
    "screen" to "FightDetails"
))

// Log de navigation
GrafanaLogger.logScreenView("FightDetails", "Home")

// Log de performance
val startTime = System.currentTimeMillis()
doExpensiveOperation()
val duration = System.currentTimeMillis() - startTime
GrafanaLogger.logPerformance("expensive_operation", duration)
```

### Intercepteur réseau

```kotlin
import com.example.mobile_uber_fight.logger.NetworkLoggingInterceptor
import okhttp3.OkHttpClient

// Configurer OkHttp avec l'intercepteur
val client = OkHttpClient.Builder()
    .addInterceptor(NetworkLoggingInterceptor())
    .build()

// Toutes les requêtes seront automatiquement loggées
```

### Nettoyage

```kotlin
// Dans Application.onTerminate() ou Activity.onDestroy()
GrafanaLogger.shutdown()
```

---

## 🔍 Querying Logs in Grafana

### Accès à Grafana Cloud

1. Connectez-vous à [Grafana Cloud](https://grafana.com)
2. Allez dans **Explore** → **Logs**
3. Sélectionnez la source **grafanacloud-logs**

### Requêtes LogQL utiles

```logql
# Tous les logs du backend
{service_name="uber-fight-backend"}

# Logs d'erreur uniquement
{service_name="uber-fight-backend"} |= "ERROR"

# Logs d'une fonction spécifique
{service_name="uber-fight-functions"} | json | functionName="createFight"

# Logs mobile
{service_name="uber-fight-backend"} | json | source="android"

# Logs de paiement
{service_name="uber-fight-backend"} | json | category="payment"

# Erreurs des dernières 24h
{service_name=~"uber-fight.*"} |= "ERROR" | json

# Requêtes lentes (>3s)
{service_name="uber-fight-backend"} | json | duration > 3000

# Logs par utilisateur
{service_name="uber-fight-backend"} | json | userId="user_123"
```

### Créer des alertes

1. Allez dans **Alerting** → **Alert rules**
2. Créez une règle basée sur une requête LogQL
3. Exemple : Alerter si >10 erreurs en 5 minutes

```logql
sum(count_over_time({service_name="uber-fight-backend"} |= "ERROR" [5m])) > 10
```

---

## 🔧 Troubleshooting

### Les logs n'arrivent pas dans Grafana

1. **Vérifiez les variables d'environnement**
   ```bash
   # Backend
   echo $GRAFANA_INSTANCE_ID
   echo $GRAFANA_API_KEY
   ```

2. **Testez avec la route de test**
   ```bash
   curl https://votre-app.vercel.app/api/test-grafana
   ```

3. **Vérifiez les logs console**
   - Cherchez `[GrafanaLogger]` dans les logs serveur

4. **Vérifiez l'authentification**
   - L'API key doit commencer par `glc_`
   - L'instance ID doit être un nombre

### Erreur "Rate limit exceeded"

L'API `/api/logs` limite à 100 requêtes/minute par IP. Solutions :
- Réduire la fréquence des logs côté client
- Utiliser `sendBatchLogs` pour grouper les logs

### Les logs sont tronqués

- Les messages sont limités à 2000 caractères
- Les attributs string sont limités à 500 caractères
- Le stack trace est limité à 500/1000 caractères

### Mobile : Logs non envoyés

1. Vérifiez la connectivité réseau
2. Vérifiez que l'URL de l'API est correcte dans `GrafanaLogger.kt`
3. Vérifiez les permissions INTERNET dans `AndroidManifest.xml`

---

## ✅ Best Practices

### ✅ À faire

- **Logger les événements métier importants** (fights, payments, auth)
- **Inclure du contexte** (userId, fightId, etc.)
- **Utiliser les bons niveaux** (info, warn, error, debug)
- **Logger les performances** des opérations critiques
- **Logger les erreurs avec stack traces**
- **Sanitizer les données** avant logging

### ❌ À éviter

- **NE JAMAIS logger** : mots de passe, tokens, données bancaires, PII
- **NE PAS logger** depuis le client directement vers Grafana
- **NE PAS logger** trop fréquemment (rate limiting)
- **NE PAS bloquer** l'app si le logging échoue
- **NE PAS logger** les données volumineuses (images, fichiers)

### Structure recommandée des logs

```json
{
  "message": "[Category] Action descriptive",
  "level": "info",
  "attributes": {
    "category": "fight|payment|auth|performance|network",
    "userId": "user_123",
    "entityId": "fight_456",
    "action": "created|updated|deleted",
    "duration": 250,
    "status": "success|failed"
  }
}
```
---

## 🔗 Liens utiles

- [Grafana Cloud](https://grafana.com/products/cloud/)
- [OpenTelemetry Protocol (OTLP)](https://opentelemetry.io/docs/specs/otlp/)
- [LogQL Documentation](https://grafana.com/docs/loki/latest/logql/)
- [Next.js Middleware](https://nextjs.org/docs/app/building-your-application/routing/middleware)
