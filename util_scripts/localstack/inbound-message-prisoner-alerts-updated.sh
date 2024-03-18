#!/bin/bash

read -r -p "Please enter the prisoner number: " PRISONER_NUMBER

if [ -z "$PRISONER_NUMBER" ]
then
  echo "No prisoner number specified."
  exit 99
fi

read -r -p "Please enter the booking ID: " BOOKING_ID

if [ -z "$BOOKING_ID" ]
then
  echo "No booking id specified."
  exit 99
fi

aws --endpoint-url=http://localhost:4566 sns publish --topic-arn arn:aws:sns:eu-west-2:000000000000:domainevents-topic --message '{"eventType":"prisoner-offender-search.prisoner.alerts-updated","version":"1.0","occurredAt":"2020-02-12T15:14:24.125533+00:00", "publishedAt":"2020-02-12T15:15:09.902048716+00:00","description":"Prisoners alerts have been updated", "personReference":{"identifiers":[{"type":"NOMS","value":"'"$PRISONER_NUMBER"'"}]}, "additionalInformation":{"nomsNumber":"'"$PRISONER_NUMBER"'", "bookingId":"'"$BOOKING_ID"'","alertsAdded": ["A1", "A2"],"alertsRemoved": ["R1", "R2"]}}' --message-attributes '{"eventType":{"DataType":"String","StringValue":"prisoner-offender-search.prisoner.alerts-updated"}}'
