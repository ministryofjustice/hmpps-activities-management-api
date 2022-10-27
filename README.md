[![codecov](https://codecov.io/github/ministryofjustice/hmpps-activities-management-api/branch/main/graph/badge.svg?token=DYVHRRP1B1)](https://codecov.io/github/ministryofjustice/hmpps-activities-management-api)
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/ministryofjustice/hmpps-activities-management-api/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/ministryofjustice/hmpps-activities-management-api/tree/main)

# hmpps-activities-management-api

This services provide access to the endpoints for the management of prisoner related activities. The main client is the
[activities management UI](https://github.com/ministryofjustice/hmpps-activities-management) located here.

This service requires a postgresql database.

## Building the project

Tools required:

* JDK v18+
* Kotlin (Intellij)
* docker
* docker-compose

## Install gradle

`$ ./gradlew`

`$ ./gradlew clean build`

## Running the service

Start up the docker dependencies using the docker-compose file in the `hmpps-activities-management-api` service.

There is a script to help, which sets local profiles, port and DB connection properties to the
values required.

`$ ./run-local.sh`

Or, to run with default properties set in the docker-compose file

`$ docker-compose pull && docker-compose up`

Or, to use default port and properties

`$ SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun`


## Running the unit tests

Unit tests mock all external dependencies and can be run with no dependent containers.

`$ ./gradlew test`
