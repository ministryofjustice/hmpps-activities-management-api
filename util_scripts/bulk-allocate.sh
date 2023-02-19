#!/bin/bash

read -r -p "Please enter the environment for the bulk allocations dev/preprod/prod: " ENVIRONMENT

function getHosts() {
  if [ "$ENVIRONMENT" = "prod" ]; then
    AUTH_HOST="https://sign-in.hmpps.service.justice.gov.uk"
    API_HOST="https://activities-api.prison.service.justice.gov.uk"
  else
    AUTH_HOST="https://sign-in-$ENVIRONMENT.hmpps.service.justice.gov.uk"
    API_HOST="https://activities-api-$ENVIRONMENT.prison.service.justice.gov.uk"
  fi

 echo Using Auth host $AUTH_HOST
 echo Useing API host $API_HOST
}

function getCsvFile() {
  read -r -p "Please enter the batch allocations CSV filename: " BATCH_ALLOCATIONS_CSV_FILE

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

function getSchedule() {
  read -r -p "Please enter the schedule ID: " SCHEDULE_ID

  STATUS=$(curl -s -o /dev/null -I -w "%{http_code}" "$API_HOST/schedules/$SCHEDULE_ID" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN")

  if [ "$STATUS" != "200" ]; then
    echo HTTP STATUS CODE "$STATUS" - error reading schedule ID "${SCHEDULE_ID}"
    exit 1
  fi
}

function allocatePrisoners() {
  while IFS="," read -r prison_number pay_band_id; do
    curl \
      -s \
      -o /dev/null \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d '{ "prisonerNumber": "'"${prison_number}"'", "payBandId": '"${pay_band_id}"' }' \
      POST "$API_HOST/schedules/$SCHEDULE_ID/allocations"
    echo ""
  done <"$BATCH_ALLOCATIONS_CSV_FILE"
}

getHosts
getCsvFile
getBearerToken
getSchedule

read -r -p "Please confirm you wish to allocate prisoners to schedule ID $SCHEDULE_ID in $ENVIRONMENT? (yes/no) " yn

case $yn in
yes) ;;
no)
  echo exiting...
  exit
  ;;
*)
  echo invalid response
  exit 1
  ;;
esac

allocatePrisoners

echo Done!
