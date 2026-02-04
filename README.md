[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-activities-management-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-activities-management-api "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-activities-management-api)
[![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://activities-api-dev.prison.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config)
[![codecov](https://codecov.io/github/ministryofjustice/hmpps-activities-management-api/branch/main/graph/badge.svg?token=DYVHRRP1B1)](https://codecov.io/github/ministryofjustice/hmpps-activities-management-api)

# hmpps-activities-management-api

This service provides access to the endpoints for the management of prisoner related activities and appointments.

The main, but not the only, client is the Activities Management UI found [here](https://github.com/ministryofjustice/hmpps-activities-management).

## Building the project

Tools required:

* JDK v25
* Kotlin (Intellij) v2.2
* PostgresSQL v17
* [Docker](https://www.docker.com/)
* [Docker Compose](https://docs.docker.com/compose/)

Useful tools that can be installed, using [Homebrew](https://brew.sh/), but are not essential:

* [kubectl](https://kubernetes.io/docs/reference/kubectl/) - not essential for building the project but will be needed for other tasks.
* [k9s](https://k9scli.io/) - a terminal-based UI to interact with your Kubernetes clusters.
* [jq](https://jqlang.github.io/jq/) - a lightweight and flexible command-line JSON processor.
* [AWS CLI](https://aws.amazon.com/cli/) - useful if running [LocalStack](https://www.localstack.cloud/), interrogating queues, etc.

## Install gradle and build the project

```
./gradlew
```

```
./gradlew clean build
```

## Running the service locally

Add a local `.env` file to the root of the project:

#### Set up the local environment variables
```
SYSTEM_CLIENT_ID=<system.client.id>
SYSTEM_CLIENT_SECRET=<system.client.secret>
DB_NAME=activities-management-db
DB_PASS=activities-management
DB_SERVER=localhost:<port>>
DB_SSL_MODE=prefer
DB_USER=activities-management
DPR_PASSWORD=dpr_password
DPR_USER=dpr_user
FEATURE_CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED=true
```

- You **must** escape any '\$' characters with '\\$'
- `SYSTEM_CLIENT_ID` and `SYSTEM_CLIENT_SECRET` can be extracted from the Kubernetes secrets for the `DEV` environment.
- `DB_SERVER` should include the port of the local Postgres DB Docker container.

#### Run LocalStack and Postgres Docker containers
```
docker-compose up --remove-orphans
```

There is a script to help, which sets local profiles, port and DB connection properties to the
values required.

#### Run the service
```
./run-local.sh
```

Or, to use default port and properties
```
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```
#### Optionally - configure AWS

You might need to set up your AWS config and credentials files:

Sample config file content
```
[profile default]
region = eu-west-2
```

Sample credentials file content
```
[default]
aws_access_key_id = foo
aws_secret_access_key = bar
```

## Utilities

There are some example scripts to simulate publishing and consuming messages in the [util_scripts/localstack folder](util_scripts/localstack).

## Running tests

### Unit

```
./gradlew test 
 ```

### Integration

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

#### Ktint

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

## Dependencies

This service is dependent on the following services:

* [Auth API](https://sign-in-dev.hmpps.service.justice.gov.uk/auth/swagger-ui/index.html) - authorisation and authentication
* [Activities Management API](https://activities-api-dev.prison.service.justice.gov.uk/swagger-ui/index.html#/) - activities management api
* [Case Notes API](https://dev.offender-case-notes.service.justice.gov.uk/swagger-ui/index.html#/) - case notes api
* [Incentives API](https://incentives-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html#/) - incentives api
* [Locations Inside Prison API](https://locations-inside-prison-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html#/) - locations inside prison api
* [Manage Adjudications API](https://manage-adjudications-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html#/) - manage adjudications api
* [Nomis Mapping API](https://nomis-sync-prisoner-mapping-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html#/) - nomis mapping api
* [Non-Associations API](https://non-associations-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html#/) - non-associations api
* [Prison API](https://prison-api-dev.prison.service.justice.gov.uk/swagger-ui/index.html#/) - prison api
* [Prisoner Search API](https://prisoner-search-dev.prison.service.justice.gov.uk/swagger-ui/index.html#/) - prisoner search api

## Runbook

### Re-running a job

**IMPORTANT: extreme caution should be taken if the job failure in question is in production and probably warrants two developers working together.**

There may be times on overnight job fails to run. To re-run it you can port forward onto a running pod and make a `curl` request to the job in question.

If for example the attendance creation failed it can be re-run as follows:

In a terminal window port forward to one of the API pods.

```shell
  kubectl -n hmpps-activities-management-<dev|preprod|prod> get pods | grep api
```
Using one of the API pods listed from the command above do the following
```
kubectl -n hmpps-activities-management-<dev|preprod|prod> port-forward hmpps-activities-management-api-<POD_SUFFIX_GOES_HERE> 8080:8080
```

In another terminal window a curl command to run the job (the one below is running attendance creation)

```shell
  curl -XPOST "http://localhost:8080/job/manage-attendance-records?date=2023-11-18&prisonCode=XYZ"
```

### Manually raising events

**IMPORTANT: As a rule of thumb, once people are out of the prison in question, we don't want to publish events for them. If in doubt, check with the team responsible for the [sync](https://github.com/ministryofjustice/hmpps-prisoner-to-nomis-update) service.**

There may be times we need to manually raise an event in production e.g. on the back of a bug fix.

In a terminal window port forward to one of the API pods

```shell
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

#### Building and running the docker image locally

The `Dockerfile` relies on the application being built first. Steps to build the docker image:
1. Build the jar files
```bash
./gradlew clean assemble
```
2. Copy the jar files to the base directory so that the docker build can find them
```bash
cp build/libs/*.jar .
```
3. Build the docker image with required arguments
```bash
docker build -t activities-api .
```
4. Run the docker image
```bash
./run-docker-local.sh
```
