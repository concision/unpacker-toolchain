#!/usr/bin/env sh

# run packages api server
# shellcheck disable=SC2039
uvicorn src.main:app \
  --host 0.0.0.0 \
  --port 80 \
  "$([ "${DEVELOPMENT}" == "true" ] && echo --reload)"
