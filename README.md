# Dragons of Mugloar

A fullstack player for [Dragons of Mugloar](https://dragonsofmugloar.com/): a Vue 3 + TypeScript
SPA backed by a Spring Boot proxy for the official game API
(`https://dragonsofmugloar.com/api/v2`).

The app supports two modes:

- **Manual** - play by hand: solve tasks, buy shop items, investigate your reputation.
- **Automatic** - a backend-driven bot plays out a whole game, one move per second. Its strategy
  reaches about 5000 points fairly consistently (roughly 3000-8000 depending on luck).

## Architecture & Strategy

The Spring Boot backend proxies the game API and owns all game state (in memory). The Vue SPA
talks only to the backend under `/api` and renders the state it returns. The automatic player
runs in the backend one move at a time; the frontend just calls `next-move` once per second.

The bot's strategy: heal when low on lives, buy level-up items while affordable (keeping a gold
reserve), otherwise solve the ad with the highest `probability × reward`.

## Requirements

- Java 25
- Node 24.12+

## Run

```bash
cd backend && ./mvnw spring-boot:run       # API server on http://localhost:8080

cd frontend && npm install && npm run dev  # SPA on Vite's port, /api proxied to :8080
```

Open the printed Vite URL (usually http://localhost:5173). Frontend and backend are same-origin
in dev (Vite proxy) and are expected to be deployed behind the same origin.

## Test

```bash
cd backend && ./mvnw test
cd frontend && npm run test:unit -- --run
cd frontend && npm run type-check
```
