#!/bin/bash

set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FRESH_START=false
STRIPE_LISTEN_LOG="/tmp/casino-stripe-listen.log"
PAYMENT_ENV_FILE="/tmp/casino-payment-service-env.sh"

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

shell_quote() {
    printf "%q" "$1"
}

load_local_env() {
    local env_file="$ROOT_DIR/.env"

    if [ ! -f "$env_file" ]; then
        return
    fi

    echo "Loading local .env file..."
    echo "Note: .env is local only and must not be committed."

    while IFS= read -r line || [ -n "$line" ]; do
        case "$line" in
            ""|\#*)
                continue
                ;;
            STRIPE_SECRET_KEY=*)
                STRIPE_SECRET_KEY="${line#STRIPE_SECRET_KEY=}"
                STRIPE_SECRET_KEY="${STRIPE_SECRET_KEY%\"}"
                STRIPE_SECRET_KEY="${STRIPE_SECRET_KEY#\"}"
                STRIPE_SECRET_KEY="${STRIPE_SECRET_KEY%\'}"
                STRIPE_SECRET_KEY="${STRIPE_SECRET_KEY#\'}"
                export STRIPE_SECRET_KEY
                ;;
        esac
    done < "$env_file"
}

start_stripe_cli() {
    local has_stripe_secret=true

    if [ -z "${STRIPE_SECRET_KEY:-}" ]; then
        echo "WARNING: STRIPE_SECRET_KEY is not set. Stripe checkout provider will not work."
        has_stripe_secret=false
    fi

    if ! command -v stripe >/dev/null 2>&1; then
        echo "WARNING: Stripe CLI not found. Skipping Stripe webhook listener."
        return
    fi

    if [ "$has_stripe_secret" = false ]; then
        return
    fi

    echo "Starting Stripe CLI webhook listener..."
    : > "$STRIPE_LISTEN_LOG"

    local quoted_root_dir
    local quoted_log_file
    quoted_root_dir="$(shell_quote "$ROOT_DIR")"
    quoted_log_file="$(shell_quote "$STRIPE_LISTEN_LOG")"

    osascript <<EOF
tell application "Terminal"
    activate
    do script "printf '\\\\033]0;stripe-cli\\\\007'; cd $quoted_root_dir && echo 'Starting Stripe CLI webhook listener...' && stripe listen --forward-to localhost:8080/api/payments/webhooks/stripe 2>&1 | tee $quoted_log_file"
end tell
EOF

    for _ in {1..15}; do
        STRIPE_WEBHOOK_SECRET="$(grep -Eo 'whsec_[A-Za-z0-9_]+' "$STRIPE_LISTEN_LOG" 2>/dev/null | head -n 1 || true)"
        if [ -n "$STRIPE_WEBHOOK_SECRET" ]; then
            export STRIPE_WEBHOOK_SECRET
            echo "Stripe CLI started and webhook secret captured."
            return
        fi

        sleep 1
    done

    echo "WARNING: Could not capture Stripe webhook secret. Stripe webhooks may fail."
}

write_payment_env_file() {
    : > "$PAYMENT_ENV_FILE"
    chmod 600 "$PAYMENT_ENV_FILE"

    # STRIPE_SECRET_KEY comes from local .env or the shell environment.
    # STRIPE_WEBHOOK_SECRET is captured from Stripe CLI output.
    # These secrets are written only to a temporary local file for the payment-service terminal.
    # Do not commit real Stripe secrets.
    if [ -n "${STRIPE_SECRET_KEY:-}" ]; then
        printf "export STRIPE_SECRET_KEY=%s\n" "$(shell_quote "$STRIPE_SECRET_KEY")" >> "$PAYMENT_ENV_FILE"
    fi
    if [ -n "${STRIPE_WEBHOOK_SECRET:-}" ]; then
        printf "export STRIPE_WEBHOOK_SECRET=%s\n" "$(shell_quote "$STRIPE_WEBHOOK_SECRET")" >> "$PAYMENT_ENV_FILE"
    fi
}

load_local_env
start_stripe_cli
write_payment_env_file

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
echo "payment-service: http://localhost:8085"
echo "notification-service: http://localhost:8086"
echo "RabbitMQ UI: http://localhost:15672"
echo "pgAdmin: http://localhost:5050"
echo ""
docker compose ps

ROOT_DIR_QUOTED="$(shell_quote "$ROOT_DIR")"
PAYMENT_ENV_FILE_QUOTED="$(shell_quote "$PAYMENT_ENV_FILE")"

osascript <<EOF
tell application "Terminal"
    activate

    do script "printf '\\\\033]0;Docker Infra\\\\007'; cd $ROOT_DIR_QUOTED && echo 'Docker Infra is running.' && docker compose ps"

    do script "printf '\\\\033]0;api-gateway\\\\007'; cd $ROOT_DIR_QUOTED/api-gateway && echo 'Starting api-gateway on port 8080...' && ./mvnw spring-boot:run"

    do script "printf '\\\\033]0;auth-service\\\\007'; cd $ROOT_DIR_QUOTED/auth-service && echo 'Starting auth-service on port 8081...' && ./mvnw spring-boot:run"

    do script "printf '\\\\033]0;user-service\\\\007'; cd $ROOT_DIR_QUOTED/user-service && echo 'Starting user-service on port 8082...' && ./mvnw spring-boot:run"

    do script "printf '\\\\033]0;wallet-service\\\\007'; cd $ROOT_DIR_QUOTED/wallet-service && echo 'Starting wallet-service on port 8083...' && ./mvnw spring-boot:run"

    do script "printf '\\\\033]0;payment-service\\\\007'; . $PAYMENT_ENV_FILE_QUOTED && rm -f $PAYMENT_ENV_FILE_QUOTED; cd $ROOT_DIR_QUOTED/payment-service && echo 'Starting payment-service...' && ./mvnw spring-boot:run"

    do script "printf '\\\\033]0;notification-service\\\\007'; cd $ROOT_DIR_QUOTED/notification-service && echo 'Starting notification-service...' && ./mvnw spring-boot:run"
end tell
EOF
