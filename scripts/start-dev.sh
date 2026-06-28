#!/bin/bash

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

osascript <<EOF
tell application "Terminal"
    activate

    do script "cd '$ROOT_DIR' && echo 'Starting Docker infrastructure...' && docker compose up -d && echo '' && echo 'Infrastructure started.' && echo 'api-gateway: http://localhost:8080' && echo 'auth-service: http://localhost:8081' && echo 'user-service: http://localhost:8082' && echo 'RabbitMQ UI: http://localhost:15672' && echo 'pgAdmin: http://localhost:5050' && echo '' && docker compose ps"

    do script "cd '$ROOT_DIR/api-gateway' && echo 'Starting api-gateway on port 8080...' && ./mvnw spring-boot:run"

    do script "cd '$ROOT_DIR/auth-service' && echo 'Starting auth-service on port 8081...' && ./mvnw spring-boot:run"

    do script "cd '$ROOT_DIR/user-service' && echo 'Starting user-service on port 8082...' && ./mvnw spring-boot:run"
end tell
EOF
