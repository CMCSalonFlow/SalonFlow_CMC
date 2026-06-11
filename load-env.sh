#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 development|staging|production"
  exit 1
fi

PROFILE="$1"
case "$PROFILE" in
  development|staging|production)
    ;;
  *)
    echo "Invalid profile: $PROFILE"
    exit 2
    ;;
esac

ENV_FILE="backend/.env.$PROFILE"
if [ ! -f "$ENV_FILE" ]; then
  echo "Missing env file: $ENV_FILE"
  exit 3
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

NODE_ENV="${NODE_ENV:-$PROFILE}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-$PROFILE}"
export NODE_ENV
export SPRING_PROFILES_ACTIVE

echo "Loaded environment file: $ENV_FILE"
echo "NODE_ENV=$NODE_ENV"
echo "SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE"

echo "Starting backend app with profile: $PROFILE"
cd backend
./mvnw spring-boot:run
