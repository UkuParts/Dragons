# frontend/AGENTS.md

Vue 3 + TypeScript SPA (Vite, Vitest). Talks only to the backend under `/api`; Vite proxies
`/api` to `http://localhost:8080` in dev.

## Commands

```bash
npm run dev                 # dev server with /api proxy
npm run test:unit -- --run  # run unit tests once
npm run type-check          # vue-tsc --build
npm run build               # type-check + production build
npm run format              # prettier, experimental CLI
```

## Structure

- `src/types/game.ts` - shared domain types. Add new API shapes here.
- `src/services/http.ts` - fetch wrapper; throws `ApiError` with the backend message and a parsed
  `Retry-After` when present. Applies a request timeout (`VITE_API_TIMEOUT_MS`, default 120s, longer
  than the backend's worst-case retry window) and converts network failures and invalid JSON bodies
  into `ApiError`s too.
- `src/services/gameApi.ts` - backend game endpoints. The only place that calls `http` for game
  actions; every call returns the full backend-owned `GameState`.
- `src/services/autoApi.ts` - the automatic `nextMove(gameId)` call.
- `src/composables/useGame.ts` - singleton manual game view: holds the latest `GameState` from the
  backend and exposes it as computed refs (`game`, `tasks`, `shopItems`, `reputation`,
  `reputationTurn`, `lastMessage`, `lastMessageFailed`). Actions (`startGame`, `ensureGame`,
  `refreshState`, `solveTask`, `buyItem`, `investigateReputation`) replace the state with the
  backend response; on a failed action it reloads `GET /api/games/{gameId}`, and on 410 it marks
  the stored game over. `ensureGame()` starts a game if there is none and otherwise resyncs.
- `src/composables/useAutoGame.ts` - singleton automatic-run viewer: `start()` calls
  `gameApi.startGame()` (also the restart action), then calls `autoApi.nextMove(gameId)` once per
  `ACTION_PAUSE_MS` (1000ms) until the backend reports `finished`; `stop()` cancels the loop on
  unmount. Transient move failures are retried up to `MAX_CONSECUTIVE_ERRORS` (5) times, pausing
  for a forwarded `Retry-After` when present and `ACTION_PAUSE_MS` otherwise, before giving up.
  Exposes `snapshot`, `error`, `running`, `starting`, `stopReason`, `start` and `stop`; `stopReason`
  comes from `AutoMoveResult.reason` and drives the end-of-run message in `AutomaticGameView`.
- `src/router/index.ts` - `vue-router` setup, exporting the `routes` array (used by tests with a
  memory history). Routes: `/` (HomeView), `/manual`, `/automatic`, unknown paths redirect home.
- `src/components/` - presentational components (props in, events out). `TaskBoard` and `ShopPanel`
  accept `readOnly` to hide their action buttons for automatic play.
- `src/utils/probability.ts` - maps API probability strings to safety rank and risk level, ordered
  safest first.
- `src/views/` - `HomeView` (mode selection), `ManualGameView`, `AutomaticGameView` (watches a
  backend run, read-only board and shop, restart and back-home buttons).

## Rules

- Keep API calls in `services/`; components must not use `fetch`, `gameApi` or `autoApi` directly.
- Navigate with `vue-router` (`RouterLink`, `useRouter`). Views do not emit navigation events and
  `App.vue` only renders `RouterView`.
- The backend owns the game state; the frontend never merges state locally. `useGame` and
  `useAutoGame` replace their state refs with the `GameState` returned by the backend.
- `useGame` and `useAutoGame` state is module-level (singleton) so it survives navigation between
  views. `AutomaticGameView` starts a run on mount and calls `stop()` on unmount; the manual view
  calls `ensureGame()` on mount.
- Every backend action returns the refreshed board and shop, so the frontend does not fetch them
  separately. Encrypted tasks are decoded by the backend, so ad ids and probability labels are
  already usable. Reputation is only updated by the explicit `investigateReputation` action.
- Tests live in `src/**/__tests__/*.spec.ts` (excluded from the app tsconfig, included by the
  vitest tsconfig). Use `@vue/test-utils` and `vi.mock('@/services/gameApi')` (or
  `@/services/autoApi`). Views that use router links are mounted with a `createMemoryHistory()`
  router built from the exported `routes`. `useAutoGame` tests use fake timers because the run is
  paced with `setTimeout`.
- Format with `npm run format` before finishing; style is no semicolons, single quotes,
  100-char width.
