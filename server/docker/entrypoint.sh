#!/usr/bin/env sh

# run packages api server
uvicorn src.main:app --host 0.0.0.0 --port 80
