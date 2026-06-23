# Auth Flow

This document describes the basic authentication flow used in the `auth-service`.

## 1. Registration

User creates an account using:

```http
POST /auth/register
```

The backend receives email and password.

Backend actions:

- validates request data
- checks if email already exists
- hashes password using BCrypt
- creates user with default role `USER`
- saves user in `auth_users` table

The raw password is never stored in the database.

## 2. Login

User logs in using:

```http
POST /auth/login
```

Backend actions:

- finds user by email
- checks password using BCrypt
- checks if account is active
- generates JWT access token
- generates refresh token
- stores refresh token hash in `refresh_tokens` table
- returns tokens to the client

Response contains:

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "refresh-token",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "user-id",
    "email": "user@example.com",
    "role": "USER",
    "status": "ACTIVE"
  }
}
```

## 3. Protected Requests

For protected endpoints, the client sends the access token in the `Authorization` header:

```http
Authorization: Bearer <accessToken>
```

Backend actions:

- reads JWT from the request header
- validates token signature and expiration
- extracts user email and role
- loads user from database
- allows request if token is valid

Example:

```http
GET /auth/me
Authorization: Bearer <accessToken>
```

## 4. Refresh Token

When the access token expires, the client sends refresh token to:

```http
POST /auth/refresh
```

Request:

```json
{
  "refreshToken": "refresh-token"
}
```

Backend actions:

- hashes received refresh token
- finds token hash in database
- checks if token exists
- checks if token is not expired
- checks if token is not revoked
- generates new access token
- rotates refresh token
- marks old refresh token as revoked
- creates and returns new refresh token

This means every refresh creates a new refresh token and invalidates the old one.

## 5. Login Again

When the same user logs in again:

- old active refresh tokens for that user are revoked
- new access token is generated
- new refresh token is generated
- only the latest refresh token remains active

Old refresh tokens stay in the database with:

```text
revoked = true
```

This keeps history while preventing old tokens from being reused.

## 6. Logout

User logs out using:

```http
POST /auth/logout
```

Request:

```json
{
  "refreshToken": "refresh-token"
}
```

Backend actions:

- validates refresh token
- marks refresh token as revoked
- returns `204 No Content`

After logout, the refresh token can no longer be used to get a new access token.

The access token may still work until it expires, because access tokens are stateless and are not stored in the database.

## 7. Token Storage

Access token:

- stored on the client side
- used for protected API requests
- short lifetime
- not stored in database

Refresh token:

- returned to the client
- stored in database as hash
- longer lifetime
- can be revoked
- used only to get a new access token

## Summary

Main flow:

```text
Register -> Login -> Access protected endpoints -> Refresh token when access token expires -> Logout
```

Token usage:

```text
Access token  -> used on every protected API request
Refresh token -> used only on /auth/refresh and /auth/logout
```