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
- `src/services/http.ts` - fetch wrapper; throws `ApiError` with the backend message. Applies a
  request timeout (`VITE_API_TIMEOUT_MS`, default 120s, longer than the backend's worst-case retry
  window) and converts network failures and invalid JSON bodies into `ApiError`s too.
- `src/services/gameApi.ts` - backend game endpoints. The only place that calls `http` for game
  actions.
- `src/composables/useGame.ts` - singleton game store: state refs, actions
  (`startGame`, `refreshBoard`, `solveTask`, `buyItem`, `investigateReputation`), loading/error
  flags, `lastMessage` / `lastMessageFailed` (the API's outcome text for the latest action and
  whether it was a failure) and `reputationTurn` (the turn the reputation was last fetched on, since
  the API's reputation response has no turn). Shared by manual and future automatic play.
- `src/router/index.ts` - `vue-router` setup, exporting the `routes` array (used by tests with a
  memory history). Routes: `/` (HomeView), `/manual`, `/automatic`, unknown paths redirect home.
- `src/components/` - presentational components (props in, events out).
- `src/utils/probability.ts` - maps API probability strings to safety rank and risk level, ordered
  safest first.
- `src/utils/decrypt.ts` - decodes base64 (rot13 fallback) fields of encrypted tasks.
- `src/views/` - `HomeView` (mode selection), `ManualGameView`, `AutomaticGameView` (placeholder).

## Rules

- Keep API calls in `services/`; components must not use `fetch` or `gameApi` directly.
- Navigate with `vue-router` (`RouterLink`, `useRouter`). Views do not emit navigation events and
  `App.vue` only renders `RouterView`.
- Game state changes belong in `useGame`; when adding the automatic player, drive it through
  `useGame` actions and reuse the existing components.
- `useGame` state is module-level (singleton) so it survives navigation between views. Reset
  state through `startGame` rather than clearing refs from outside.
- After a solve/buy, update local stats from the response and call `refreshBoard()` to reload
  tasks and shop (both free). Board fetches decode encrypted tasks with `decodeTask` before
  storing them, so solving uses the decoded ad id. Reputation is not reloaded automatically
  because investigating it costs a turn; use the explicit `investigateReputation` action instead.
- Tests live in `src/**/__tests__/*.spec.ts` (excluded from the app tsconfig, included by the
  vitest tsconfig). Use `@vue/test-utils` and `vi.mock('@/services/gameApi')`. Views that use
  router links are mounted with a `createMemoryHistory()` router built from the exported `routes`.
- Format with `npm run format` before finishing; style is no semicolons, single quotes,
  100-char width.
