#!/bin/sh
set -e
printf '%s' "${MONGO_REPLICA_KEY:?MONGO_REPLICA_KEY is required}" | base64 | tr -d '\n' > /data/keyfile
chmod 400 /data/keyfile
chown mongodb:mongodb /data/keyfile
exec docker-entrypoint.sh "$@"
