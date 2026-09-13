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
- `game/dto/` - Java records mirroring external API payloads.

## Endpoints

| Method | Path                                          | Upstream call                  |
| ------ | --------------------------------------------- | ------------------------------ |
| POST   | `/api/games`                                  | start a new game               |
| GET    | `/api/games/{gameId}/messages`                | message board                  |
| POST   | `/api/games/{gameId}/solve/{adId}`            | solve a task (costs a turn)    |
| GET    | `/api/games/{gameId}/shop`                    | shop listing                   |
| POST   | `/api/games/{gameId}/shop/buy/{itemId}`       | buy an item (costs a turn)     |
| POST   | `/api/games/{gameId}/investigate/reputation`  | reputation (costs a turn)      |

## Rules

- Keep API access in `DragonsApiClient`; do not call `RestClient` from controllers.
- Keep DTOs as records matching the external JSON exactly. Reputation values are `double`.
- Add controller tests with `@WebMvcTest(GameController.class)` and
  `@MockitoBean DragonsApiClient` (see `src/test/java/com/test/dragons/game/`). HTTP-level
  client tests use `MockRestServiceServer`.
- The backend is stateless: it does not persist games or player state. The frontend owns the
  live game state.
- If an automatic player is added server-side, add it as a new service/controller rather than
  putting logic in `GameController`.
