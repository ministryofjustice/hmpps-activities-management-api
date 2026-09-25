#!/bin/bash

read -r -p "Please enter the surviving prisoner number: " PRISONER_NUMBER

if [ -z "$PRISONER_NUMBER" ]
then
  echo "No prisoner number specified."
  exit 99
fi

read -r -p "Please enter the removed prisoner number: " REMOVED_PRISONER_NUMBER

if [ -z "$REMOVED_PRISONER_NUMBER" ]
then
  echo "No removed prisoner number specified."
  exit 99
fi

aws --endpoint-url=http://localhost:14566 sns publish --topic-arn arn:aws:sns:eu-west-2:000000000000:domainevents-topic --message '{"eventType":"prison-offender-events.prisoner.merged","version":"1.0","occurredAt":"2020-02-12T15:14:24.125533+00:00", "publishedAt":"2020-02-12T15:15:09.902048716+00:00","description":"A prisoner has been merged", "personReference":{"identifiers":[{"type":"NOMS","value":"'"$PRISONER_NUMBER"'"}]}, "additionalInformation":{"nomsNumber":"'"$PRISONER_NUMBER"'", "removedNomsNumber":"'"$REMOVED_PRISONER_NUMBER"'"}}' --message-attributes '{"eventType":{"DataType":"String","StringValue":"prison-offender-events.prisoner.merged"}}'
