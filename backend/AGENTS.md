# backend/AGENTS.md

Spring Boot proxy for the Dragons of Mugloar game API. Java 25, Spring Boot 4.1.1, Maven
wrapper (`./mvnw`).

## Commands

```bash
./mvnw test              # compile + run tests
./mvnw spring-boot:run   # dev server on :8080
./mvnw clean package     # build jar
```

## Structure

- `src/main/java/com/test/dragons/BackendApplication.java` - entry point.
- `config/DragonsApiConfig.java` - `RestClient` bean bound to `dragons.api.*` (base URL,
  timeouts). Values come from `application.properties`; the base URL is overridable with the
  `DRAGONS_API_BASE_URL` environment variable.
- `game/DragonsApiClient.java` - the only place that knows the external API paths. Retries HTTP
  429 honoring `Retry-After` in seconds or HTTP-date form (falling back to a linear backoff,
  capped at 30s) because rate-limited requests are never processed; game actions are not
  idempotent, so other errors are not retried. The app runs on virtual threads
  (`spring.threads.virtual.enabled=true`), so the blocking retry sleep does not tie up platform
  threads.
- `game/GameController.java` - frontend-facing REST API under `/api/games`.
- `game/GameApiExceptionHandler.java` - forwards 4xx responses as-is with the upstream JSON message
  (`error` or `status` field) as the frontend-facing `error`; 5xx and non-standard statuses become
  502, and `Retry-After` is forwarded as well.
- `game/GameService.java` + `game/ActiveGame.java` - the single in-memory game store. `start()`
  replaces the active game and loads the free board/shop best-effort, so a refresh failure still
  leaves a usable game; `solve`, `buy` and `investigateReputation` call upstream, update the
  stored state, refresh the free board/shop best-effort and return the full `dto.GameState`. Only
  one game is active at a time, and a game id that is not the active one returns 404. On a 410 the
  stored game is marked over and the error is rethrown; on other action failures the board is
  refreshed best-effort before rethrowing. `ActiveGame` also owns the automatic-mode counters
  (consecutive level purchases and waits, where a wait is a skip purchase or a move that consumed
  no turn), so they reset with `start()`; the auto player reads and updates them through
  `GameService`.
- `game/AdDecoder.java` - base64 (rot13 fallback) decoder for encrypted ads. Board fetches are
  decoded before they are stored, so neither the frontend nor the strategy sees encrypted fields.
- `game/dto/` - Java records mirroring external API payloads, plus `GameState`, the full
  frontend-facing view (game stats, decoded tasks, shop, reputation, last message).
- `auto/AutoStrategy.java` - pure automatic-play policy.
  Returns `Solve`, `Buy` or `Skip` (buying an unknown item id to wait out an unsolvable board).
- `auto/AutoPlayer.java` - executes exactly one policy move per `nextMove(gameId)` call through
  `GameService`. It owns termination (game over, run end turn, dead board, skip limit) and returns
  `dto.AutoMoveResult(finished, reason, state)`. Recoverable 4xx upstream failures are logged and
  the refreshed state is returned so the run continues; a failed action that consumes no turn
  counts toward the same 25-wait limit and is not recorded as a purchase. 5xx and network failures
  propagate so the frontend can retry. The frontend owns the pacing.
- `auto/AutoGameController.java` - frontend-facing automatic REST API under `/api/auto/games`.
- `auto/dto/` - automatic move result record plus the `StopReason` enum (`GAME_OVER`, `TURN_LIMIT`,
  `BOARD_DEAD`, `SKIP_LIMIT`).

## Endpoints

| Method | Path                                          | Upstream call                  |
| ------ | --------------------------------------------- | ------------------------------ |
| POST   | `/api/games`                                  | start a new game               |
| GET    | `/api/games/{gameId}`                         | the stored `GameState`         |
| POST   | `/api/games/{gameId}/solve/{adId}`            | solve a task (costs a turn)    |
| POST   | `/api/games/{gameId}/shop/buy/{itemId}`       | buy an item (costs a turn)     |
| POST   | `/api/games/{gameId}/investigate/reputation`  | reputation (costs a turn)      |
| POST   | `/api/auto/games/{gameId}/next-move`          | one automatic policy move      |

## Rules

- Keep API access in `DragonsApiClient`; do not call `RestClient` from controllers.
- Keep DTOs as records matching the external JSON exactly. Reputation values are `double`.
- Add controller tests with `@WebMvcTest(...)` and `@MockitoBean GameService` / `AutoPlayer`; see
  `src/test/java/com/test/dragons/`. HTTP-level client tests use `MockRestServiceServer`.
- The backend owns the game state for both modes in memory. There is one active game at a time and
  it is not persisted; starting a new game replaces the previous one. The frontend only renders
  the returned `GameState`.
- Automatic play logic belongs in `auto/`; keep it out of `GameController` and `DragonsApiClient`.
