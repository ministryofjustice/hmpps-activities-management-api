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

## Temporary Non-Residential Location Remap (SAA-4624)

The ingress-only `POST /job/remap-non-residential-locations` trigger runs a bundled CSV migration asynchronously.
The CronJob is disabled and suspended by default. It does not create, rename or archive Locations records.

Add mapping files to `src/main/resources/location-mappings/` and deploy the application before selecting them.
Use the Locations team's comma-separated format, including `prisonId`, `locationType`, `locationId` and
`chosenChildId`. Quoted commas and CRLF are supported. Both `PARENT` and `OTHER_CHILD` rows are processed;
the archive columns and `chosenChildHasActivities` do not filter out appointment updates.
Never use the preprod mapping in production: newly created destination GUIDs differ between environments.

### Run Configuration

A data-changing run is any run with `dryRun=false`, in any environment, including both preprod and production.
Before a data-changing run, explicitly set the API deployment environment variable `LOCATION_REMAP_ALLOWED_FILE`
under `generic-service.env` in the target environment's Helm values file
([preprod](helm_deploy/values-preprod.yaml) or [production](helm_deploy/values-prod.yaml)), then deploy that configuration.
Set it to the approved CSV filename, including `.csv` but without any directory path.
For example, when using the supplied preprod file, set:

```text
LOCATION_REMAP_ALLOWED_FILE=MAPB-924-preprod-mapping-2026-10-16.csv
```

Use the filename of the approved production mapping for production runs, not the preprod filename above.
The default empty value blocks data-changing runs in both preprod and production.

Dry runs (`dryRun=true`) do not require filename approval and never change activity or appointment data or
publish domain events.

The job moves locations on activities/schedules that have not ended, including future-starting activities.
For appointments and appointment sets, future means `start_date >` the run's current date (tomorrow onwards).
Future cancelled appointments are included without changing their cancellation status. Historical, today's and
deleted appointments remain unchanged. Individual location overrides are preserved unless their ID also appears
as a source in the selected CSV. Active-series headers are updated even when the series started in the past.
The job sets `dps_location_id` to the chosen child's DPS GUID. It also uses the NOMIS Mapping API to find that
child's numeric NOMIS location ID and stores it in `internal_location_id`, so both identifiers refer to the same
destination location. It does not store a GUID in the numeric NOMIS ID field. Activity schedules' cached location
codes and descriptions are also updated to the chosen child's code and description.

### Running And Checking

1. Agree the run with the Locations team. Ensure destination locations and their required services exist.
  A preprod database restore may require Locations to rerun its preparation and provide a fresh mapping.
2. Choose the target namespace and CSV file. Use `prisons=GMI,EEI,CLI` for the pilot (Guys Marsh, Erlestoke,
  Coldingley), or `ALL` for the full set. For the one-off Job approach, deploy the CronJob with
  `cron.locationRemap.enabled=true`, `suspend=true`, `dryRun=true` and a non-empty `file` value.
  The tools-container approach does not require a CronJob.
3. Start with a dry run using either [a one-off Job](#one-off-job-with-overrides) or
  [curl from a tools container](#curl-from-a-tools-container). Both examples below use the preprod pilot.
4. Review API logs for the selected file, scope, candidate IDs and destination preflight failures. A missing,
  inactive, wrong-prison or unsuitable destination aborts before writes. Unused mapping rows are no-ops.
5. Set and deploy `LOCATION_REMAP_ALLOWED_FILE` as described in [Run Configuration](#run-configuration) above.
  Once the API rollout finishes, repeat the chosen approach with the same file and prisons, but set `DRY_RUN=false`
  for the one-off Job or `dryRun=false` in the curl request.
  For a scheduled run, explicitly set the agreed `schedule` and unsuspend only for that run.
  The backend prevents overlapping migrations.
6. Wait for the backend `job` row with `job_type='REMAP_NON_RESIDENTIAL_LOCATIONS'` to have `ended_at` set and
  `successful=true`. HTTP 202 and Kubernetes trigger completion only acknowledge submission, not completion.
  Review the final changed/failure/residual counts and SNS/NOMIS reconciliation before frontend checks.
7. Run preprod pilot, preprod full, production pilot, then production full, with manual validation between stages.
  Keep the CronJob suspended/disabled between runs and disable it after completion. Avoid concurrent user edits;
  check for newly created source references again before authorising the Locations team to archive anything.

Independent object failures are logged and processing continues, but the backend job is marked failed if any
update fails or eligible source references remain. Reruns are data-idempotent, not event-delivery retries:
the existing services commit before publishing SNS messages. A publication failure can therefore leave changed
database rows and missing events. Use the logged schedule/appointment-attendee IDs and existing failed-event
telemetry to arrange event replay/reconciliation before signing off; a no-op rerun cannot repair missing events.

### One-Off Job With Overrides

Run this from your local terminal with kubectl access to the target namespace. The CronJob must already be
deployed, but can remain suspended. Generate a Job from it, override the three environment variables locally,
then create that Job:

```sh
NAMESPACE=hmpps-activities-management-preprod
JOB_NAME="location-remap-$(date +%s)"

kubectl -n "$NAMESPACE" create job "$JOB_NAME" \
  --from=cronjob/remap-non-residential-locations \
  --dry-run=client -o yaml |
kubectl set env --local -f - \
  --containers=location-remap-trigger \
  MAPPING_FILE=MAPB-924-preprod-mapping-2026-10-16.csv \
  PRISONS=GMI,EEI,CLI \
  DRY_RUN=true \
  -o yaml |
kubectl -n "$NAMESPACE" create -f -
```

These overrides affect only the new Job; they do not change the CronJob or API deployment configuration.
`--dry-run=client` only generates the Job manifest: the final command creates and starts it. The migration's
own dry-run behaviour is controlled separately by `DRY_RUN=true`. Use a new Job name for each invocation.
For production, change the namespace and mapping filename. The new Job runs curl for you.

### Curl From A Tools Container

Run this from your local terminal to open a shell in a temporary tools container in preprod:

```sh
kubectl -n hmpps-activities-management-preprod run --rm -it debug \
  --image=ghcr.io/ministryofjustice/hmpps-devops-tools:latest \
  --restart=Never \
  --overrides='{"spec":{"serviceAccountName":"hmpps-activities-management-api"}}' \
  --command -- bash
```

At the container's shell prompt, trigger the migration:

```sh
curl --fail-with-body --request POST \
  --data-urlencode 'file=MAPB-924-preprod-mapping-2026-10-16.csv' \
  --data-urlencode 'prisons=GMI,EEI,CLI' \
  --data-urlencode 'dryRun=true' \
  http://hmpps-activities-management-api/job/remap-non-residential-locations
```

For production, use the production namespace in the kubectl command and its mapping filename in the curl request.
Type `exit` when finished; `--rm` removes the temporary pod. If a pod named `debug` already exists, use another
pod name. This approach calls the API directly and does not require deploying or enabling the CronJob.

### Committed Change Logs

Each changed schedule, appointment, series header and set header emits one INFO log beginning
`Location remap committed`, followed by a JSON object. These logs are emitted only after the database transaction
commits, and before outbound events are published, so a later publishing failure does not hide a committed change.
Dry runs, rolled-back updates and already-migrated records do not emit committed-change logs.

The JSON includes `objectType`, `objectId`, `prisonCode`, `name`, `oldDpsLocationId`, `newDpsLocationId`,
`oldLocationName`, `newLocationName`, `oldNomisLocationId` and `newNomisLocationId`.
Schedules include the owning `activityId`, activity summary as `name`, and schedule `description`.
Appointments and series include `appointmentSeriesId`, `appointmentType` (individual/group), and
`appointmentSetId` where applicable. The appointment display name uses its custom name, category description,
or category code, in that order. Sets include their set ID and display name. Appointment notes and prisoner
information are not included.

Old schedule names come from their stored description. For appointments, series and sets, the old name is read
from Locations, falling back to the CSV `locationName`, or explicitly `Unavailable` if neither exists.
New names come from the destination location. Names are JSON-escaped so quotes, commas and newlines remain
unambiguous. Use GUIDs and object IDs, not names, to identify the records: duplicate locations can share a name.

Retain these logs and the input CSV outside normal log-retention limits until cleanup is signed off.
They record the per-object old/new values needed to plan a reversal, not an automatic undo operation.
Any reversal should check for subsequent user edits and use the normal service/event publishing paths.

### App Insights Results

Each job execution submits `SAA-Location-Remap-Job-Result`, including failures before updates start.
Properties identify `runId`, `file`, `prisons`, `dryRun`, `status` (`SUCCEEDED`, `FAILED` or `DRY_RUN`),
the last `stage`, and `errorType` on failure. The run ID is also logged at the start and end of the job.
Metrics include `totalSucceeded` and `totalFailed`: counts of successful/failed update calls, each operating on
one activity, appointment series or appointment set for one mapping, not counts of CSV rows or SNS messages.
`totalChanged` counts schedules, appointments and set headers reported changed by successful calls; it does not
include series-header-only changes or changes committed by calls that subsequently fail to publish events.
`totalMappings`, `totalCandidates`, `preflightFailures`, `residualMappings` and `durationMs` provide context.
Dry runs have zero update totals. An early abort can also have zero update totals: always check `status` and
`stage`, and check `preflightFailures` for destination validation errors. Results are also logged; telemetry
submission failure is logged without changing the migration outcome. App Insights ingestion is not guaranteed.

```kusto
customEvents
| where name == "SAA-Location-Remap-Job-Result"
| project timestamp,
  runId = tostring(customDimensions.runId),
  file = tostring(customDimensions.file),
  prisons = tostring(customDimensions.prisons),
  dryRun = tostring(customDimensions.dryRun),
  status = tostring(customDimensions.status),
  stage = tostring(customDimensions.stage),
  totalSucceeded = todouble(customMeasurements.totalSucceeded),
  totalFailed = todouble(customMeasurements.totalFailed),
  preflightFailures = todouble(customMeasurements.preflightFailures),
  residualMappings = todouble(customMeasurements.residualMappings)
| order by timestamp desc
```

Preflight also submits one `SAA-Location-Remap-Preflight-Failed` event per failed mapping, including during dry
runs. These events share the result event's `runId` and include the file, scope, prison, source/destination DPS
IDs and location names where available, plus `errorType` and `reason`. Measurements show the affected activity,
appointment-series and appointment-set counts. Each event reports the mapping's first validation failure;
validation continues across the remaining mappings. Telemetry submission errors do not interrupt preflight.

```kusto
customEvents
| where timestamp > ago(24h)
| where name == "SAA-Location-Remap-Preflight-Failed"
// Add a runId filter to select one run's failures.
| project timestamp,
  runId = tostring(customDimensions.runId),
  file = tostring(customDimensions.file),
  dryRun = tostring(customDimensions.dryRun),
  prison = tostring(customDimensions.prisonId),
  sourceId = tostring(customDimensions.sourceDpsLocationId),
  sourceName = tostring(customDimensions.sourceLocationName),
  destinationId = tostring(customDimensions.destinationDpsLocationId),
  destinationName = tostring(customDimensions.destinationLocationName),
  reason = tostring(customDimensions.reason),
  errorType = tostring(customDimensions.errorType),
  activities = todouble(customMeasurements.activityCount),
  series = todouble(customMeasurements.appointmentSeriesCount),
  sets = todouble(customMeasurements.appointmentSetCount)
| order by timestamp desc
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
* [External Movements API](https://external-movements-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html#/) - external movements api

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
