# Dragons of Mugloar - frontend

Vue 3 + TypeScript SPA (Vite, Vitest) for the Dragons of Mugloar player. It only talks to the
Spring Boot backend under `/api`; the Vite dev server proxies `/api` to `http://localhost:8080`.

## Project setup

```sh
npm install
```

### Development

Run the backend on `:8080`, then:

```sh
npm run dev
```

### Tests and checks

```sh
npm run test:unit -- --run  # unit tests once
npm run type-check          # vue-tsc
npm run build               # type-check + production build
npm run format              # prettier
```

### Routes

- `/` - mode selection
- `/manual` - manual play
- `/automatic` - automatic play (placeholder)
