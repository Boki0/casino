# Project Plan

## Project Goal

The goal of this project is to build a backend system for a casino simulation platform using Spring Boot and microservice architecture.

The platform will use only virtual credits and will be created for learning and portfolio purposes.

## Main Features

### Authentication and Authorization

Users must be logged in to use the platform.

Supported roles:

- USER
- ADMIN
- SUPPORT

### User Features

A USER can:

- register
- log in
- view profile
- view wallet balance
- play demo games
- view own transactions
- contact support

### Admin Features

An ADMIN can:

- view users
- manage users
- manage games
- view transactions
- view audit logs
- close tickets

### Support Features

A SUPPORT user can:

- view support tickets
- reply to users
- open tickets
- view basic user information in read-only mode

## Development Phases

### Phase 1 — Project Setup

- create Git repository
- add documentation
- define architecture
- define Git workflow
- prepare base project structure

### Phase 2 — Auth Service

- user registration
- user login
- password hashing
- JWT token generation
- roles: USER, ADMIN, SUPPORT

### Phase 3 — API Gateway

- single entry point for frontend
- route requests to services
- validate JWT token
- protect private endpoints

### Phase 4 — User Service

- user profile
- account status
- admin user overview
- support read-only user view

### Phase 5 — Wallet Service

- virtual balance
- demo deposit
- balance check
- reserve and update balance

### Phase 6 — Game Service

- list demo games
- play demo game
- calculate result
- connect with wallet service

### Phase 7 — Transaction Service

- save transaction history
- show user transactions
- show admin transaction overview

### Phase 8 — Support Service

- create ticket
- reply to ticket
- close ticket
- support dashboard

### Phase 9 — Audit Logging

- log important actions
- track admin actions
- track balance changes
- track login events

### Phase 10 — Production-like Improvements

- Docker Compose
- Redis
- RabbitMQ or Kafka
- monitoring
- centralized logs
- tests
- CI/CD