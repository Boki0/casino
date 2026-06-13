# Git Workflow

## Branches

The project will use the following branches:

- main
- dev
- feature/*

## Branch Purpose

### main

Stable version of the project.

Code should not be pushed directly to main.

### dev

Development branch.

Finished features are merged into dev first.

### feature branches

Each new feature should be developed in a separate branch.

Examples:

- feature/auth-service
- feature/api-gateway
- feature/wallet-service
- feature/game-service

## Commit Message Format

The project will use clear commit messages.

Format:

```text
type(scope): message
```

Examples:

```text
docs(project): add initial project plan
chore(project): add base repository structure
feat(auth): add user registration
feat(auth): add JWT login
feat(wallet): add balance endpoint
fix(auth): handle invalid credentials
test(wallet): add balance service tests
```

## Commit Types

- docs — documentation changes
- chore — project setup, configuration, dependencies
- feat — new feature
- fix — bug fix
- test — tests
- refactor — code improvement without changing behavior

## Development Flow

```text
main
  |
dev
  |
feature/auth-service
```

Steps:

1. create feature branch from dev
2. implement feature
3. commit changes
4. merge feature branch into dev
5. merge dev into main when stable