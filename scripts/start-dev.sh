#!/bin/bash

set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FRESH_START=false

usage() {
    echo "Usage: ./scripts/start-dev.sh [--fresh]"
    echo ""
    echo "Options:"
    echo "  --fresh    Reset local PostgreSQL public schema before starting services."
}

for arg in "$@"; do
    case "$arg" in
        --fresh)
            FRESH_START=true
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $arg"
            usage
            exit 1
            ;;
    esac
done

wait_for_postgres() {
    echo "Waiting for Postgres container to be ready..."

    until docker exec casino-postgres pg_isready -U casino_user -d casino_platform >/dev/null 2>&1; do
        sleep 1
    done

    echo "Postgres is ready."
}

reset_database() {
    echo "WARNING: --fresh will delete all local database tables/data."
    echo "Resetting public schema in casino_platform..."

    docker exec casino-postgres psql \
        -U casino_user \
        -d casino_platform \
        -v ON_ERROR_STOP=1 \
        -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

    echo "Database schema reset complete."
}

echo "Starting Docker infrastructure..."
cd "$ROOT_DIR"
docker compose up -d
echo "Infrastructure started."

wait_for_postgres

if [ "$FRESH_START" = true ]; then
    reset_database
fi

echo ""
echo "Service URLs:"
echo "api-gateway: http://localhost:8080"
echo "auth-service: http://localhost:8081"
echo "user-service: http://localhost:8082"
echo "wallet-service: http://localhost:8083"
echo "RabbitMQ UI: http://localhost:15672"
echo "pgAdmin: http://localhost:5050"
echo ""
docker compose ps

osascript <<EOF
tell application "Terminal"
    activate

    do script "printf '\\\\033]0;Docker Infra\\\\007'; cd '$ROOT_DIR' && echo 'Docker Infra is running.' && docker compose ps"

    do script "printf '\\\\033]0;api-gateway\\\\007'; cd '$ROOT_DIR/api-gateway' && echo 'Starting api-gateway on port 8080...' && ./mvnw spring-boot:run"

    do script "printf '\\\\033]0;auth-service\\\\007'; cd '$ROOT_DIR/auth-service' && echo 'Starting auth-service on port 8081...' && ./mvnw spring-boot:run"

    do script "printf '\\\\033]0;user-service\\\\007'; cd '$ROOT_DIR/user-service' && echo 'Starting user-service on port 8082...' && ./mvnw spring-boot:run"

    do script "printf '\\\\033]0;wallet-service\\\\007'; cd '$ROOT_DIR/wallet-service' && echo 'Starting wallet-service on port 8083...' && ./mvnw spring-boot:run"
end tell
EOF
