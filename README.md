[![codecov](https://codecov.io/github/ministryofjustice/hmpps-activities-management-api/branch/main/graph/badge.svg?token=DYVHRRP1B1)](https://codecov.io/github/ministryofjustice/hmpps-activities-management-api)
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/ministryofjustice/hmpps-activities-management-api/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/ministryofjustice/hmpps-activities-management-api/tree/main)

# hmpps-activities-management-api

This services provide access to the endpoints for the management of prisoner related activities. The main client is the
[activities management UI](https://github.com/ministryofjustice/hmpps-activities-management) located here.

This service requires a postgresql database.

## Building the project

Tools required:

* JDK v19+
* Kotlin (Intellij)
* docker
* docker-compose
* AWS cli

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

Ports

| Service                   | Port |  
|---------------------------|------|
| activities-management-api | 8080 |
| activities-management-db  | 5432 |
| hmpps-auth                | 8090 |
| prison-api                | 8091 |
| prison-api                | 8091 |
| localstack                | 4566 |

To create a Token (local):
```
curl --location --request POST "http://localhost:8081/auth/oauth/token?grant_type=client_credentials" --header "Authorization: Basic $(echo -n {Client}:{ClientSecret} | base64)"
```

To simulate AWS SQS/SNS mode you need to have the localstack container running:

```
$ docker-compose -f docker-compose.yml -f  docker-compose-localstack.yml up

$ ./run-localstack.sh
```

To list the localstack queue attributes:

```
$ aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes --attribute-names All --queue-url http://localhost:4566/000000000000/domainevents-queue
```

To read a message off the queue (if there are any):

```
$ aws --endpoint-url=http://localhost:4566 sqs receive-message --queue-url http://localhost:4566/000000000000/domainevents-queue
```

## Swagger v3
Visit Scheduler
```
http://localhost:8080/swagger-ui/index.html
```

Export Spec
```
http://localhost:8080/v3/api-docs?group=full-api
```

## Running the unit tests

Unit tests mock all external dependencies and can be run with no dependent containers.

`$ ./gradlew test`

## Common gradle tasks

To list project dependencies, run:

```
./gradlew dependencies
``` 

To check for dependency updates, run:
```
./gradlew dependencyUpdates --warning-mode all
```

To run an OWASP dependency check, run:
```
./gradlew clean dependencyCheckAnalyze --info
```

To upgrade the gradle wrapper version, run:
```
./gradlew wrapper --gradle-version=<VERSION>
```

To automatically update project dependencies, run:
```
./gradlew useLatestVersions
```

#### Ktlint Gradle Tasks

To run Ktlint check:
```
./gradlew ktlintCheck
```

To run Ktlint format:
```
./gradlew ktlintFormat
```

To apply ktlint styles to intellij
```
./gradlew ktlintApplyToIdea
```

To register pre-commit check to run Ktlint format:
```
./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook 
```

...or to register pre-commit check to only run Ktlint check:
```
./gradlew ktlintApplyToIdea addKtlintCheckGitPreCommitHook
```
