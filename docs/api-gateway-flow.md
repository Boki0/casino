# API Gateway JWT and Internal Header Flow

## Overview

`api-gateway` is the single public entry point for client HTTP requests.

```text
Client
  -> api-gateway
  -> auth-service / user-service
```

Services run locally on:

- API Gateway: `http://localhost:8080`
- Auth service: `http://localhost:8081`
- User service: `http://localhost:8082`

Clients should call backend APIs through `api-gateway`, not directly through
individual backend services.

## Routes

Gateway routes and rewrites requests like this:

```text
POST /api/auth/register -> auth-service POST /auth/register
POST /api/auth/login    -> auth-service POST /auth/login
POST /api/auth/refresh  -> auth-service POST /auth/refresh
POST /api/auth/logout   -> auth-service POST /auth/logout

GET  /api/users/me      -> user-service GET /users/me
PUT  /api/users/me      -> user-service PUT /users/me
```

General route mapping:

```text
/api/auth/**  -> auth-service /auth/**
/api/users/** -> user-service /users/**
```

## Public Routes

These routes do not require a JWT access token:

- `/api/auth/register`
- `/api/auth/login`
- `/api/auth/refresh`
- `/api/auth/logout`

They are forwarded by `api-gateway` to `auth-service`.

## Protected Routes

Protected routes require an access token:

```http
Authorization: Bearer <access_token>
```

Currently protected:

- `/api/users/**`

Future non-auth routes should also be protected by default.

## JWT Validation

`auth-service` creates JWT access tokens and refresh tokens.

For protected routes:

1. Client sends the access token to `api-gateway`.
2. `api-gateway` validates the JWT access token.
3. `api-gateway` checks token signature and expiration.
4. `api-gateway` extracts authenticated user context from the JWT:
   - `userId`
   - email
   - role
5. `api-gateway` forwards the request to the downstream service.

`user-service` does not validate JWT directly. It trusts the authenticated user
context only when the request also contains the valid internal gateway secret.

## Propagated Headers

After validating the JWT, `api-gateway` adds trusted user context headers:

```http
X-Auth-User-Id: <auth user id>
X-Auth-User-Role: <role>
X-Auth-User-Email: <email>
```

Before adding these headers, `api-gateway` removes any client-provided values for:

- `X-Auth-User-Id`
- `X-Auth-User-Role`
- `X-Auth-User-Email`

This prevents clients from spoofing authenticated user identity.

## Internal Gateway Secret

`api-gateway` also adds:

```http
X-Internal-Gateway-Secret: <configured internal secret>
```

`user-service` requires this header for `/users/**` routes.

Direct calls to `user-service` without this header return:

```text
401 Unauthorized
```

This is a simple local development service-to-service protection. In production,
it should be combined with private networking, environment-provided secrets and,
if needed, mTLS or another stronger service identity mechanism.

## Example Curl Commands

Register through the gateway:

```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "player@example.com",
    "password": "Password123!",
    "username": "player1"
  }'
```

Login through the gateway:

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "player@example.com",
    "password": "Password123!"
  }'
```

Access the current user profile through the gateway with a valid JWT:

```bash
curl -i http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <access_token>"
```

Access the current user profile through the gateway without a token:

```bash
curl -i http://localhost:8080/api/users/me
```

Expected result:

```text
401 Unauthorized
```

Direct call to `user-service` without the internal gateway secret:

```bash
curl -i http://localhost:8082/users/me \
  -H "X-Auth-User-Id: 00000000-0000-0000-0000-000000000000"
```

Expected result:

```text
401 Unauthorized
```

## Notes

- Refresh token logic is still handled by `auth-service`.
- `api-gateway` does not store users.
- `api-gateway` does not store refresh tokens.
- `api-gateway` has no database.
- `user-service` trusts gateway-propagated user context only when
  `X-Internal-Gateway-Secret` is valid.
