# Uber Fight

Plateforme de mise en relation entre clients et combattants. Le projet est compose de trois parties :

- **Backend** — Dashboard admin en Next.js
- **Mobile** — Application Android native (Kotlin)
- **Functions** — Firebase Cloud Functions (notifications, metriques, gestion users)

## Mobile

Telecharger l'APK depuis les [GitHub Releases](https://github.com/thierrymgn/Uber-Fight/releases) et l'installer sur un appareil Android.

## Backend (Dashboard)

### Pre-requis

- Node.js 18+
- pnpm

### Lancement

```bash
cd backend
cp .env.example .env.local  # remplir les valeurs
pnpm install
pnpm dev
```

Le dashboard est accessible sur `http://localhost:3000`.

## Variables d'environnement

Voir les fichiers `.env.example` dans `backend/` et `functions/` pour la liste complete des variables requises.

Les principales :

- **Firebase** — cles API, projet, service account admin
- **Grafana Cloud** — instance ID, API key, endpoints OTLP (logs + metriques)
