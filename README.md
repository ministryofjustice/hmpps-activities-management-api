[![codecov](https://codecov.io/github/ministryofjustice/hmpps-activities-management-api/branch/main/graph/badge.svg?token=DYVHRRP1B1)](https://codecov.io/github/ministryofjustice/hmpps-activities-management-api)
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/ministryofjustice/hmpps-activities-management-api/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/ministryofjustice/hmpps-activities-management-api/tree/main)
[![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://activities-api-dev.prison.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config)

# hmpps-activities-management-api

This services provide access to the endpoints for the management of prisoner related activities. The main client is the
[activities management UI](https://github.com/ministryofjustice/hmpps-activities-management) located here.

This service requires a postgresql database.

## Building the project

Tools required:

* JDK v21+
* Kotlin (Intellij)
* docker
* docker-compose
* AWS cli
* kubectl

## Install gradle

```
./gradlew
```

```
./gradlew clean build
```

## Environment variables

The system client id and secret used to retrieve the OAuth 2.0 Access Token needed for service to service API calls can be set as local environment variables.
This allows API calls made from this service that do not use the caller's token to successfully authenticate.
The create scheduled instances and records jobs also require an environment variable to specify the days ahead to create.

The process of adding the environment variables to your local development environment varies based on OS and shell used.
For zsh, add the following lines to your `.zprofile` file replacing the text in quotes with the dev client id and secret: 

```
export SYSTEM_CLIENT_ID="<system.client.id>"
export SYSTEM_CLIENT_SECRET="<system.client.secret>"
export SCHEDULE_AHEAD_DAYS=46
```

N.B. you must escape any '$' characters with '\\$'

## Service Alerting

The serivce uses [Sentry.IO](https://ministryofjustice.sentry.io/) to raise alerts in Slack and email for job failures. There is a project and team set up in Sentry specifically for this service called `#hmpps-activities-management`. You can log in (and register if need be) with your MoJ github account [here](https://ministryofjustice.sentry.io/).

Rules for alerts can be configured [here](https://ministryofjustice.sentry.io/alerts/rules/).

For Sentry integration to work it requires the environment variable `SENTRY_DSN` to be set up in Kubernettes. This value for this can be found [here](https://ministryofjustice.sentry.io/settings/projects/hmpps-activities-management/keys/).

```
echo -n '<SENTRY_DSN_GOES_HERE>' | base64
```

Create a file called sentry.yaml based on the example below and add the base 64 encoded to it:

```
apiVersion: v1
kind: Secret
metadata:
  name: sentry
type: Opaque
data:
  SENTRY_DSN: <BASE_64_ENCODED_SENTRY_DSN_GOES_HERE>
```

Apply the secret to each environment with `kubectl` using the file above as required:

```
kubectl -n hmpps-activities-management-<dev|preprod|prod> apply -f sentry.yaml
```

## Running the service

Start up the docker dependencies using the docker-compose file in the `hmpps-activities-management-api` service.

There is a script to help, which sets local profiles, port and DB connection properties to the
values required.

```
./run-local.sh
```

Or, to run with default properties set in the docker-compose file
```
docker-compose pull && docker-compose up
```

Or, to use default port and properties

```
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Ports

| Service                   | Port |  
|---------------------------|------|
| activities-management-api | 8080 |
| activities-management-db  | 5432 |
| hmpps-auth                | 8090 |
| prison-api                | 8091 |
| localstack                | 4566 |

To create a Token (running the local profile):
```
curl --location --request POST "http://localhost:8081/auth/oauth/token?grant_type=client_credentials" --header "Authorization: Basic $(echo -n {Client}:{ClientSecret} | base64)"
```

To simulate AWS SQS/SNS mode you need to have the localstack container running:

```
docker-compose -f docker-compose-localstack.yml up
```

```
./run-localstack.sh
```

There are some example scripts in the util_scripts/localstack folder.

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

To run integration tests use below command
```
./gradlew integrationTest
```