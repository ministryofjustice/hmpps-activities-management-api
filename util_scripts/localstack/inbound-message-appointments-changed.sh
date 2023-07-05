#!/bin/bash

read -r -p "Enter the prisoner number: " PRISONER_NUMBER

if [ -z "$PRISONER_NUMBER" ]
then
  echo "No prisoner number specified."
  exit 99
fi

read -r -p "Enter the prison code: " PRISONER_CODE

if [ -z "$PRISONER_CODE" ]
then
  echo "No prison code specified."
  exit 99
fi

read -r -p "Enter the action required - YES or NO: " ACTION

if [ -z "$ACTION" ]
then
  echo "No action specified."
  exit 99
fi

aws --endpoint-url=http://localhost:4566 sns publish --topic-arn arn:aws:sns:eu-west-2:000000000000:domainevents-topic --message '{"eventType":"prison-offender-events.prisoner.appointments-changed","version":"1.0","description":"A prisoner''''s appointments have been changed","occurredAt":"2023-05-09T13:41:25+01:00","publishedAt":"2023-05-09T13:41:25.688466232+01:00","personReference":{"identifiers":[{"type":"NOMS","value":"'"$PRISONER_NUMBER"'"}]},"additionalInformation":{"action":"'"$ACTION"'","prisonId":"'"$PRISONER_CODE"'","user":"SOME_USER"}}' --message-attributes '{"eventType":{"DataType":"String","StringValue":"prison-offender-events.prisoner.appointments-changed"}}'
