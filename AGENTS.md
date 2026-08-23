# AGENTS.md

Instructions for AI coding agents working in this project.

## What this is

An ERP (Enterprise Resource Planning) system with two apps in this repo:

- `Back_End/` - Java 21 / Spring Boot 4 REST API (Maven), Spring Data JPA
- `Front_End/erpfront/` - Next.js 16 / React 19 / TypeScript frontend (pnpm)

A Universal ERP platform (in the spirit of Odoo) for businesses of any type -
retail, manufacturing, service, e-commerce, and more - unifying core
operations (inventory, sales, purchasing, accounting, POS, etc.) behind one
multi-tenant SaaS platform without forking the database schema per industry.

## Coding conventions

### Backend (`Back_End/`)

- Java 21, Spring Boot 4 (`spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc`), built with Maven
- Lombok for boilerplate (getters/setters/constructors)
- Package root: `ERP.erpbackend`
- Layered structure: controller -> service -> repository (Spring Data JPA)
- No database is configured yet (`application.yaml` only sets the app name) - choose and wire one before persistence work starts

### Frontend (`Front_End/erpfront/`)

- Next.js 16 App Router, React 19, TypeScript strict mode
- No `src/` directory - `app/` lives at the project root (`Front_End/erpfront/app/`)
- Tailwind CSS v4, CSS-first config via `@theme` in `app/globals.css`, no `tailwind.config.js`
- Functional components and hooks only, no class components
- No component library (e.g. shadcn/ui) installed yet
- No data-fetching or auth pattern chosen yet - the frontend will call the Spring Boot API once endpoints exist

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

## Testing

- Backend: JUnit is already wired via the Spring Boot test starters (`Back_End/src/test/java/ERP/erpbackend/ErpbackendApplicationTests.java`), so `./mvnw test` is a real, working gate.
- Frontend: no unit test runner installed yet. Add one before treating frontend logic tests as a gate.
