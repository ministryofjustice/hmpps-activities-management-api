#!/bin/bash

read -r -p "Please enter the prisoner number: " PRISONER_NUMBER

if [ -z "$PRISONER_NUMBER" ]
then
  echo "No prisoner number specified."
  exit 99
fi

read -r -p "Did the incentive level change? (true/false): " LEVEL_CHANGED

if [ -z "$LEVEL_CHANGED" ]
then
  LEVEL_CHANGED=true
fi

aws --endpoint-url=http://localhost:14566 sns publish --topic-arn arn:aws:sns:eu-west-2:000000000000:domainevents-topic --message '{"eventType":"incentives.iep-review.updated","version":"1.0","occurredAt":"2020-02-12T15:14:24.125533+00:00", "publishedAt":"2020-02-12T15:15:09.902048716+00:00","description":"An incentive review has been updated", "personReference":{"identifiers":[{"type":"NOMS","value":"'"$PRISONER_NUMBER"'"}]}, "additionalInformation":{"nomsNumber":"'"$PRISONER_NUMBER"'", "incentiveLevel":"BAS","previousIncentiveLevel":"STD","incentiveLevelChanged":'"$LEVEL_CHANGED"'}}' --message-attributes '{"eventType":{"DataType":"String","StringValue":"incentives.iep-review.updated"}}'
