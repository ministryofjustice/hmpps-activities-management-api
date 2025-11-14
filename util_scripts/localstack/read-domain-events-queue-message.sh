#!/bin/bash

aws --endpoint-url=http://localhost:14566 sqs receive-message --queue-url http://localhost:4566/000000000000/domainevents-queue