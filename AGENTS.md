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

- The frontend never calls the game API directly. It calls the backend under `/api/**`
  (Vite proxies `/api` in dev), and the backend calls the game API via `DragonsApiClient`.
- Frontend navigation uses `vue-router`: `/` (mode selection), `/manual`, `/automatic`.
  `App.vue` renders only `RouterView`.
- The backend owns the single active game state (`backend/.../game/GameService.java` +
  `ActiveGame`): starting stores it, every action and `GET /api/games/{gameId}` return the full
  `GameState`, and the frontend never merges state locally.
- The automatic player runs **in the backend** one step at a time: `backend/.../auto/AutoPlayer.java`
  applies the policy in `AutoStrategy.java`: heal at 1 life, buy level items while affordable
  (keeping a 150-gold reserve, capped at 8 consecutive level purchases, until turn 115), otherwise
  solve the highest `probability × reward` ad while skipping heists, `Impossible` ads and
  `Suicide mission` ads before turn 30. When nothing
  is solvable it waits a turn (buying an unknown item id, capped at 25 consecutive waits) instead
  of ending the run, and recovers from failed actions using the refreshed state; a failed action
  that consumes no turn counts toward the same cap and is not recorded as a purchase. `AutoPlayer`
  owns termination and reports `AutoMoveResult(finished, reason, state)` with `GAME_OVER`,
  `TURN_LIMIT`, `BOARD_DEAD` or `SKIP_LIMIT`.
  `frontend/src/composables/useAutoGame.ts` calls `POST /api/auto/games/{gameId}/next-move` once
  per second until it reports `finished` (retrying transient errors up to five times, honouring
  `Retry-After` when the backend forwards one), and renders the returned state with the manual-mode
  components (`readonly` prop).
- Components under `frontend/src/components/` are presentational: props in, events out. Keep
  API calls out of them.
- Frontend and backend are same-origin in dev (Vite proxy) and are expected to be deployed behind
  the same origin; there is no CORS configuration.
- External game API quirks: solving a task, buying an item, and investigating reputation each
  consume a turn; fetching messages and the shop is free. Buying a non-existent item id also
  consumes a turn, which the auto player uses as its wait action. The reputation endpoint returns
  no turn number, so the backend increments the stored turn when it investigates. Reputation values
  can be fractional. `probability` values from the API map to risk levels in
  `frontend/src/utils/probability.ts`. Encrypted tasks (base64, rot13 fallback) are decoded by
  `backend/.../game/AdDecoder.java` before they are stored, so the frontend only sees decoded
  tasks and solves use the decoded ad id. Once the game is over the API returns HTTP 410 for
  every action; the backend marks the stored game over and forwards 410, and `useGame` reflects
  it from the refreshed state instead of refetching the board itself.

## Conventions

- Frontend: prettier config (no semicolons, single quotes, print width 100). Run
  `npm run format` after edits. Import via the `@/` alias.
- Backend: tabs for indentation, Java records for DTOs, one package per concern under
  `com.test.dragons`.
- Do not add comments unless they explain non-obvious behavior.
- After changing behavior, run the relevant tests plus `npm run build` / `./mvnw test`.
- Update the relevant AGENTS.md when architecture or commands change.
