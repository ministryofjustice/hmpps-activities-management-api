#!/bin/bash

aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes --attribute-names All --queue-url http://localhost:4566/000000000000/domainevents-queue