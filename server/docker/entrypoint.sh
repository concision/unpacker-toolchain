#!/usr/bin/env sh

while ! nc -z database 5432; do
  sleep 0.1
done

# run packages api server
# shellcheck disable=SC2039
uvicorn src.main:app \
  --host 0.0.0.0 \
  --port 80 \
  --lifespan on \
  "$([ "${DEVELOPMENT}" == "true" ] && echo --reload)"
