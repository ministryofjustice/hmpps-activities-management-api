#!/bin/bash

set -e

usage() {
  echo "Usage: $0 [-p|-pp|-d] [-r]"
  echo "  -p           Use prod environment"
  echo "  -pp          Use preprod environment"
  echo "  -d           Use dev environment (default)"
  echo "  -r           Forward to read replica database"
  echo ""
  echo "Examples:"
  echo "  $0                # dev environment, standard DB"
  echo "  $0 -r             # dev environment, read-replica DB"
  echo "  $0 -p             # prod environment, standard DB"
  echo "  $0 -p -r          # prod environment, read-replica DB"
  echo "  $0 -pp            # preprod environment, standard DB"
  echo "  $0 -pp -r         # preprod environment, read-replica DB"
  exit 1
}

ENV="dev"
READONLY=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    -p) ENV="prod"; shift ;;
    -pp) ENV="preprod"; shift ;;
    -d) ENV="dev"; shift ;;
    -r|--read-only) READONLY=true; shift ;;
    *) usage ;;
  esac
done

USER=$(whoami | sed 's/\./-/g') # Replace dots in username to avoid pod name issues
if [[ "$READONLY" == true ]]; then
  POD_NAME="port-forward-$USER-readrep"
else
  POD_NAME="port-forward-$USER"
fi
NAMESPACE="hmpps-activities-management-$ENV"

if [[ "$ENV" == "prod" ]]; then
  SECRET_NAME=$([[ "$READONLY" == true ]] && echo "activities-rds-read-replica" || echo "activities-api-rds")
else
  SECRET_NAME=$([[ "$READONLY" == true ]] && echo "activities-rds-read-replica" || echo "activities-rds")
fi

echo "Using namespace: $NAMESPACE"
echo "Using secret: $SECRET_NAME"
echo "Pod name: $POD_NAME"

DATABASE=$(kubectl -n $NAMESPACE get secrets $SECRET_NAME -o json | jq -r ".data | map_values(@base64d).rds_instance_address")
if [[ -z "$DATABASE" || "$DATABASE" == "null" ]]; then
  echo "Could not fetch database address from secret $SECRET_NAME in namespace $NAMESPACE"
  exit 1
fi

echo "Using database: $DATABASE"

kubectl -n $NAMESPACE run $POD_NAME --image=ministryofjustice/port-forward --port=5432 \
  --env="REMOTE_HOST=$DATABASE" --env="LOCAL_PORT=5432" --env="REMOTE_PORT=5432" || true

sleep 1
kubectl wait --for=condition=Ready pod/$POD_NAME -n $NAMESPACE

cleanup() {
  echo "Cleaning up pod $POD_NAME..."
  kubectl -n $NAMESPACE delete pod $POD_NAME
  echo "Done."
}
trap cleanup EXIT

echo "Starting port-forward. Press Ctrl+C when done."
kubectl -n $NAMESPACE port-forward $POD_NAME 5432:5432

