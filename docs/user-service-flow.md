# User Service Flow

`user-service` is responsible for user profile data.

It does not handle authentication, passwords, JWT tokens, roles or refresh tokens. Those belong to `auth-service`.

## Main Data

User profile contains:

- authUserId
- email
- username
- displayName
- firstName
- lastName
- country
- phoneNumber
- dateOfBirth
- avatarUrl
- refCode
- createdAt
- updatedAt

`authUserId` represents the user ID created by `auth-service`.

## Create User Profile

Endpoint:

POST /internal/users

This endpoint creates a user profile after a user is registered in `auth-service`.

For now it is tested manually from Postman. Later it will be called by `auth-service` or triggered by a `UserRegistered` event.

## Get Current User Profile

Endpoint:

GET /users/me

For now, the current user is identified using temporary header:

X-Auth-User-Id

Later this will be replaced by JWT/API Gateway integration.

## Update Current User Profile

Endpoint:

PUT /users/me

Allows updating profile fields:

- displayName
- firstName
- lastName
- country
- phoneNumber
- dateOfBirth
- avatarUrl

It does not update:

- authUserId
- email
- username
- refCode

## Temporary Authentication

`X-Auth-User-Id` is temporary and only used for local development.

In the future, the user identity will come from:

- JWT validation inside the service, or
- API Gateway forwarding authenticated user headers.