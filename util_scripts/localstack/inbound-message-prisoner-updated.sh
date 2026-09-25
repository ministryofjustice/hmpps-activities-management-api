#!/bin/bash

read -r -p "Please enter the prisoner number: " PRISONER_NUMBER

if [ -z "$PRISONER_NUMBER" ]
then
  echo "No prisoner number specified."
  exit 99
fi

read -r -p "Did the location change? (true/false): " LOCATION_CHANGED

if [ -z "$LOCATION_CHANGED" ]
then
  LOCATION_CHANGED=true
fi

if [ "$LOCATION_CHANGED" = "true" ]
then
  CATEGORIES='["LOCATION"]'
else
  CATEGORIES='["ALERTS"]'
fi

aws --endpoint-url=http://localhost:14566 sns publish --topic-arn arn:aws:sns:eu-west-2:000000000000:domainevents-topic --message '{"eventType":"prisoner-offender-search.prisoner.updated","version":"1.0","occurredAt":"2020-02-12T15:14:24.125533+00:00", "publishedAt":"2020-02-12T15:15:09.902048716+00:00","description":"A prisoner record has been updated", "personReference":{"identifiers":[{"type":"NOMS","value":"'"$PRISONER_NUMBER"'"}]}, "additionalInformation":{"nomsNumber":"'"$PRISONER_NUMBER"'", "categoriesChanged":'"$CATEGORIES"'}}' --message-attributes '{"eventType":{"DataType":"String","StringValue":"prisoner-offender-search.prisoner.updated"}}'
