# Dragons of Mugloar

Player for the Dragons of Mugloar game: a Vue 3 + TypeScript SPA and a Spring Boot backend that
proxies the official game API (`https://dragonsofmugloar.com/api/v2`).

## Run

```bash
# terminal 1 - backend on :8080
cd backend && ./mvnw spring-boot:run

# terminal 2 - frontend on Vite's port
cd frontend && npm install && npm run dev
```

## Tests

```bash
cd backend && ./mvnw test
cd frontend && npm run test:unit -- --run
cd frontend && npm run type-check
```
