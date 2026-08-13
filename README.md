# Casino Simulation Platform

Casino Simulation Platform is a microservices-based casino platform built primarily as a backend portfolio project. It demonstrates authentication, user profiles, virtual wallets and deposits, game-provider integration and callbacks, asynchronous events, and centralized routing through an API Gateway. The platform uses virtual credits only and is not intended for real-money gambling.

## Architecture and services

- **api-gateway** — Central entry point for routing, JWT authentication, and trusted internal request headers.
- **auth-service** — Registration, login, and JWT access/refresh token authentication.
- **user-service** — User profile management and profile initialization from registration events.
- **wallet-service** — Virtual balances, debit and credit operations, transaction history, and idempotent provider financial operations.
- **payment-service** — Sandbox deposit flow with Stripe Checkout and webhook handling.
- **notification-service** — Foundation for notifications and SMTP email delivery.
- **game-service** — Game catalog and provider synchronization, game launches and sessions, provider wallet callbacks, Bet and Result processing, and wallet-service integration.
- **frontend** — React UI with login and registration screens plus a Slots catalog grouped by provider. It is intentionally simpler because the project focuses on the backend.

## Key implemented flows

- **Authentication:** Register → Login → Access / Refresh token
- **Registration events:** User registered → RabbitMQ event → user profile and wallet initialization
- **Deposit:** Stripe sandbox checkout → webhook → RabbitMQ event → wallet credit
- **Game launch:** Player → API Gateway → game-service → custom mock provider → provider launch URL
- **Game play:** Provider Authenticate → Bet callback → wallet debit → Result callback → wallet credit → updated balance
- **Financial safety:** Bet and Result operations are idempotent at the wallet transaction layer.

## Technology stack

- **Backend:** Java 21, Spring Boot, Spring Security, Spring Data JPA, Maven
- **Data and infrastructure:** PostgreSQL, RabbitMQ, Docker, Docker Compose
- **Frontend:** React, TypeScript, Vite
- **Integrations:** Stripe sandbox, custom mock game provider
- **Security:** JWT access/refresh authentication, API Gateway authentication, and internal service request authentication

## Local development

Copy `.env.example` to `.env` and provide any integration credentials you want to use. Docker must be available for the local PostgreSQL and RabbitMQ infrastructure.

```bash
./scripts/start-dev.sh
```

The script starts the Docker infrastructure and opens the application services and frontend for local development.

## Current scope

The project currently focuses on microservice architecture, service-to-service communication, authentication, transaction-safe wallet operations, event-driven communication, external payment integration, and external game-provider integration. All balances use virtual credits, while payment and game-provider integrations use sandbox or mock environments.
