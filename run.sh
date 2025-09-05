#!/usr/bin/env bash
# Lightweight helper to load .env into environment and run Spring Boot (Linux/macOS/Git Bash)
# Usage: ./run.sh
set -e
ENV_FILE=".env"
if [ -f "$ENV_FILE" ]; then
  # export all KEY=VALUE lines (ignore comments and empty lines)
  set -a
  # shellcheck disable=SC2002
  cat "$ENV_FILE" | sed 's/\r$//' | grep -v '^\s*#' | grep -v '^\s*$' | while IFS= read -r line; do
    # support values with = by splitting on first =
    key="$(echo "$line" | cut -d'=' -f1)"
    val="$(echo "$line" | sed 's/^[^=]*=//')"
    export "$key=$val"
  done
  set +a
fi

# Run the app
mvn spring-boot:run

