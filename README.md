[![codecov](https://codecov.io/github/ministryofjustice/hmpps-activities-management-api/branch/main/graph/badge.svg?token=DYVHRRP1B1)](https://codecov.io/github/ministryofjustice/hmpps-activities-management-api)
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/ministryofjustice/hmpps-activities-management-api/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/ministryofjustice/hmpps-activities-management-api/tree/main)
[![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://activities-api-dev.prison.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config)

# hmpps-activities-management-api

This service provides access to the endpoints for the management of prisoner related activities and appointments.

The main (but not only) client is the Activities Management UI found [here](https://github.com/ministryofjustice/hmpps-activities-management).

## Building the project

Tools required:

* JDK v21+
* Kotlin (Intellij)
* docker
* docker-compose

Useful tools but not essential:

* KUBECTL not essential for building the project but will be needed for other tasks. Can be installed with `brew`.
* [k9s](https://k9scli.io/) a terminal based UI to interact with your Kubernetes clusters. Can be installed with `brew`.
* [jq](https://jqlang.github.io/jq/) a lightweight and flexible command-line JSON processor. Can be installed with `brew`.
* AWS CLI not essential but useful if running localstack, interrogating queues etc. Can be installed with `brew`.

## Install gradle and build the project

```
./gradlew
```

```
./gradlew clean build
```

## Running the service

There are two key environment variables needed to run the service. The system client id and secret used to retrieve the OAuth 2.0 access token needed for service to service API calls can be set as local environment variables.
This allows API calls made from this service that do not use the caller's token to successfully authenticate.

Add the following to a local `.env` file in the root folder of this project (_you can extract the credentials from the dev k8s project namespace_).

N.B. you must escape any '$' characters with '\\$'

```
export SYSTEM_CLIENT_ID="<system.client.id>"
export SYSTEM_CLIENT_SECRET="<system.client.secret>"
```

Start up the docker dependencies using the docker-compose file in the `hmpps-activities-management-api` service.

```
docker-compose up --remove-orphans
```

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

To simulate AWS SQS/SNS mode you need to have the localstack container running:

```
docker-compose -f docker-compose-localstack.yml up
```

```
./run-localstack.sh
```

There are some example scripts to simulate messages in the util_scripts/localstack folder.

## Running tests

Unit

```
./gradlew test 
 ```

Integration

```
./gradlew integrationTest
```

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

## Service Alerting

The service uses [Sentry.IO](https://ministryofjustice.sentry.io/) to raise alerts in Slack and email for job failures. There is a project and team set up in Sentry specifically for this service called `#hmpps-activities-management`. You can log in (and register if need be) with your MoJ github account [here](https://ministryofjustice.sentry.io/).

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

## Runbook

### Re-running a job

**IMPORTANT: extreme caution should be taken if the job failure in question is in production and probably warrants two developers working together.**

There may be times on overnight job fails to run. To re-run it you can port forward onto a running pod and make a `curl` request to the job in question.

If for example the attendance creation failed it can be re-run as follows:

In a terminal window port forward to one of the API pods.

```
kubectl -n hmpps-activities-management-<dev|preprod|prod> get pods | grep api
```
Using one of the API pods listed from the command above do the following
```
kubectl -n hmpps-activities-management-<dev|preprod|prod> port-forward hmpps-activities-management-api-<POD_SUFFIX_GOES_HERE> 8080:8080
```

In another terminal window a curl command to run the job (the one below is running attendance creation)

```
curl -XPOST "http://localhost:8080/job/manage-attendance-records?date=2023-11-18&prisonCode=XYZ"
```

### Manually raising events

**IMPORTANT: As a rule of thumb once people are out of the prison in question we don't want to publish events for them. If in doubt check with the team responsible for the [sync](https://github.com/ministryofjustice/hmpps-prisoner-to-nomis-update) service.**

There may be times we need to manually raise an event in production e.g. on the back of a bug fix.

In a terminal window port forward to one of the API pods

```
kubectl -n hmpps-activities-management-<dev|preprod|prod> get pods | grep api
```
Using one of the API pods listed from the command above do the following
```
kubectl -n hmpps-activities-management-<dev|preprod|prod> port-forward hmpps-activities-management-api-<POD_SUFFIX_GOES_HERE> 8080:8080
```

In another terminal window issue a curl command to publish the event e.g. PRISONER_ALLOCATION_AMENDED

```
curl --location 'http://localhost:8080/utility/publish-events' \
--header 'Content-Type: application/json' \
--data '{
    "outboundEvent":"<EVENT_TYPE_GOES_HERE>",
    "identifiers": [
       1
    ]
}'
```

### Logs

The logs are written to Application Insights and can be queried accordingly.  There is roughly a 5 minute delay before you can see them.

Some simple example Application Insights queries:

```
traces
| where cloud_RoleName == 'hmpps-activities-management-api'
```

```
traces
| where cloud_RoleName == 'hmpps-activities-management-api'
| where message has 'ABC'
```

```
union *
| where operation_Id ==‘<INSERT OPERATION ID HERE>’
|project timestamp, message, itemType, name, operation_Name, cloud_RoleName, resultCode, customDimensions, duration, url, data, innermostMessage
|sort by timestamp asc
```
