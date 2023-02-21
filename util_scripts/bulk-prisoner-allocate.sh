#!/bin/bash
#
# Script for bulk prisoner allocations using direct API calls by parsing the bulk-prisoner-allocations.csv file
# containing the schedule ids, prisoner numbers and their pay bands.
#
# File format is the schedule id, prisoner number and then pay band, for example:
#
#   1,A1234AA,1
#   1,A3444BB,2
#   ...
#
# Note: please ensure there is a carriage return on the last line of the CSV file!!
#
# Upon running you will be prompted to supply the environment (dev/preprod/prod) for the bulk allocations.
#
read -r -p "Please enter the environment for the bulk allocations dev/preprod/prod: " ENVIRONMENT

function getHosts() {
  if [ "$ENVIRONMENT" = "prod" ]; then
    AUTH_HOST="https://sign-in.hmpps.service.justice.gov.uk"
    API_HOST="https://activities-api.prison.service.justice.gov.uk"
  else
    AUTH_HOST="https://sign-in-$ENVIRONMENT.hmpps.service.justice.gov.uk"
    API_HOST="https://activities-api-$ENVIRONMENT.prison.service.justice.gov.uk"
  fi
}

function getCsvFile() {
  BATCH_ALLOCATIONS_CSV_FILE=bulk-prisoner-allocations.csv

  [ ! -f "$BATCH_ALLOCATIONS_CSV_FILE" ] && {
    echo "$BATCH_ALLOCATIONS_CSV_FILE file not found"
    exit 99
  }
}

function getBearerToken() {
  CLIENT_ID=$(kubectl -n activities-api-"$ENVIRONMENT" get secret hmpps-activities-management-api -o json | jq -r ".data | map_values(@base64d).SYSTEM_CLIENT_ID")
  CLIENT_SECRET=$(kubectl -n activities-api-"$ENVIRONMENT" get secret hmpps-activities-management-api -o json | jq -r ".data | map_values(@base64d).SYSTEM_CLIENT_SECRET")
  TOKEN=$(curl -s -X POST "$AUTH_HOST/auth/oauth/token?grant_type=client_credentials" -H "Content-Length:0" --user "$CLIENT_ID":"$CLIENT_SECRET" | jq -r '.access_token')
}

function allocatePrisoners() {
  read -r -p "Please confirm you wish to bulk allocate prisoners in $ENVIRONMENT in the bulk-prisoner-allocations.csv file? (yes/no) " yn

  case $yn in
  yes) ;;
  no)
    echo Bulk allocations cancelled!
    exit
    ;;
  *)
    echo Please respond with yes or no.
    exit 1
    ;;
  esac

  while IFS="," read -r schedule_id prison_number pay_band_id; do
    curl -s -o /dev/null \
      -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
      -d '{ "prisonerNumber": "'"${prison_number}"'", "payBandId": '"${pay_band_id}"' }' \
      POST "$API_HOST/schedules/${schedule_id}/allocations"
    echo ""

  done <"$BATCH_ALLOCATIONS_CSV_FILE"
}

getHosts
getCsvFile
getBearerToken
allocatePrisoners

echo
echo Bulk allocation of prisoners is complete
