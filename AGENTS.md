# AGENTS.md

Dragons of Mugloar player: a Vue frontend and a Spring Boot backend that proxies the official
game API at `https://dragonsofmugloar.com/api/v2`.

## Layout

- `backend/` - Spring Boot (Java 25) proxy + game API. See `backend/AGENTS.md`.
- `frontend/` - Vue 3 + TypeScript + Vite SPA. See `frontend/AGENTS.md`.

## Commands

```bash
# backend
cd backend && ./mvnw test               # run tests
cd backend && ./mvnw spring-boot:run    # dev server on :8080

# frontend
cd frontend && npm run test:unit -- --run  # run tests once
cd frontend && npm run type-check          # vue-tsc
cd frontend && npm run build               # type-check + production build
cd frontend && npm run format              # prettier (required before committing)
cd frontend && npm run dev                 # dev server, proxies /api to :8080
```

Run both servers for manual testing: backend on `:8080`, frontend on Vite's port.

## Architecture rules

- The frontend never calls the game API directly. It calls the backend under `/api/games/**`
  (Vite proxies `/api` in dev), and the backend calls the game API via `DragonsApiClient`.
- Frontend navigation uses `vue-router`: `/` (mode selection), `/manual`, `/automatic`.
  `App.vue` renders only `RouterView`.
- All game state and actions live in the singleton `frontend/src/composables/useGame.ts`.
  Manual and (future) automatic play must share it rather than reimplementing game logic.
- The automatic player does not exist yet; `AutomaticGameView.vue` is a placeholder. When
  implementing it, reuse `useGame`, `services/gameApi.ts`, the components, and the backend proxy.
- Components under `frontend/src/components/` are presentational: props in, events out. Keep
  API calls out of them.
- Frontend and backend are same-origin in dev (Vite proxy) and are expected to be deployed behind
  the same origin; there is no CORS configuration.
- External game API quirks: solving a task, buying an item, and investigating reputation each
  consume a turn; fetching messages and the shop is free. The reputation endpoint returns no
  turn number, so `useGame` increments the local turn when it investigates. Reputation values can
  be fractional. `probability` values from the API map to risk levels in
  `frontend/src/utils/probability.ts`. Encrypted tasks (base64, rot13 fallback) are decoded by
  `frontend/src/utils/decrypt.ts` before display; solves use the decoded ad id. Once the game is
  over the API returns HTTP 410 for every action; the backend forwards it as 410 and `useGame`
  marks the local game over instead of refetching.

## Conventions

- Frontend: prettier config (no semicolons, single quotes, print width 100). Run
  `npm run format` after edits. Import via the `@/` alias.
- Backend: tabs for indentation, Java records for DTOs, one package per concern under
  `com.test.dragons`.
- Do not add comments unless they explain non-obvious behavior.
- After changing behavior, run the relevant tests plus `npm run build` / `./mvnw test`.
- Update the relevant AGENTS.md when architecture or commands change.
