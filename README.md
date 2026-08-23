# ERP System

An ERP (Enterprise Resource Planning) system with two apps:

- `Back_End/` - Java 21 / Spring Boot 4 REST API (Maven)
- `Front_End/erpfront/` - Next.js 16 / React 19 / TypeScript frontend (pnpm)

## Commands

### Backend (`Back_End/`)

- Dev server: `./mvnw spring-boot:run` (`mvnw.cmd spring-boot:run` on Windows)
- Build: `./mvnw clean package`
- Test: `./mvnw test`

### Frontend (`Front_End/erpfront/`)

- Dev server: `pnpm dev` (<http://localhost:3000>)
- Build: `pnpm build`
- Production server: `pnpm start`
- Lint: `pnpm lint`
