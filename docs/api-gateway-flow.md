# API Gateway Flow

The `api-gateway` service is the public entry point for backend HTTP traffic.

## Service Port

- API Gateway: `http://localhost:8080`
- Auth service: `http://localhost:8081`
- User service: `http://localhost:8082`

## Auth Route

Incoming gateway path:

```text
/api/auth/**
```

Forwarded to:

```text
http://localhost:8081
```

Path rewrite:

```text
/api/auth/<segment> -> /auth/<segment>
```

Examples:

```text
POST http://localhost:8080/api/auth/register -> POST http://localhost:8081/auth/register
POST http://localhost:8080/api/auth/login    -> POST http://localhost:8081/auth/login
GET  http://localhost:8080/api/auth/me       -> GET  http://localhost:8081/auth/me
```

## User Route

Incoming gateway path:

```text
/api/users/**
```

Forwarded to:

```text
http://localhost:8082
```

Path rewrite:

```text
/api/users/<segment> -> /users/<segment>
```

Examples:

```text
GET http://localhost:8080/api/users/me -> GET http://localhost:8082/users/me
PUT http://localhost:8080/api/users/me -> PUT http://localhost:8082/users/me
```

## Later

JWT validation and authentication are intentionally not implemented in this step. They will be added later at the gateway layer.
