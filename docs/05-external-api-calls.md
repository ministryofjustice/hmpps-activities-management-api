# 5. Outbound external API calls

This reference covers HTTP calls made by the **Activities Management API backend**, not calls made
directly by its frontend. It describes the implemented clients and their service callers, rather
than every endpoint offered by each downstream API.

The [high-level design](high-level-design.md) lists frontend dependencies separately.
SNS / SQS messaging and S3 exports are covered by the
[synchronisation](02-nomis-sync-and-migration.md) and [CronJob](04-cron-jobs.md) documents;
they are not REST calls through the WebClients below. Telemetry exporters are also outside this
business-API inventory.

## Configuration and authentication

[`WebClientConfiguration`](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/config/WebClientConfiguration.kt)
creates the clients. Deployment URLs come from environment-specific
[Helm values](../helm_deploy); use those values for the target environment, not production URLs
when running local development.

All authenticated business clients use the API's **client credentials**, configured through
`SYSTEM_CLIENT_ID` and `SYSTEM_CLIENT_SECRET`. They do not forward the UI user's bearer token.
The OAuth2 registration selects the downstream client and requested scopes; downstream permissions
also depend on roles granted to the configured system client. This repository does not establish
the complete deployed downstream role grants.

| Dependency | Base-URL environment variable | OAuth2 registration | Requested scopes | Configured client timeout |
| --- | --- | --- | --- | --- |
| Prison API | `PRISON_API_URL` | `prison-api` | `read,write` | 10 seconds |
| Prisoner Search API | `PRISONER_SEARCH_API_URL` | `prisoner-search-api` | `read` | 10 seconds |
| Non-Associations API | `NON_ASSOCIATIONS_API_URL` | `non-associations-api` | `read` | 10 seconds |
| Incentives API | `INCENTIVES_API_URL` | `incentives-api` | `read` | 30 seconds |
| Locations Inside Prison API | `LOCATIONS_INSIDE_PRISON_API_URL` | `locations-inside-prison` | `read` | 10 seconds |
| NOMIS Mapping API | `NOMIS_MAPPING_API_URL` | `nomis-mapping-api` | `read` | 10 seconds |
| Case Notes API | `CASE_NOTES_API_URL` | `offender-case-notes-api` | `read,write` | 10 seconds |
| Manage Adjudications API | `MANAGE_ADJUDICATIONS_API_URL` | `manage-adjudications-api` | `read` | 30 seconds |
| External Movements API | `EXTERNAL_MOVEMENTS_API_URL` | `external-movements-api` | `read` | 10 seconds |
| GOV.UK bank holidays | `BANK_HOLIDAY_API_URL` (optional; default `https://www.gov.uk`) | None | Unauthenticated | 30 seconds |

These are configured defaults, not end-to-end request deadlines. Most clients use
`prison.api.timeout` (10 seconds, despite its name); Incentives, Adjudications and bank holidays
use `api.timeout` (30 seconds). Both properties can be overridden. Token acquisition has its own
`auth.timeout` default of 2 seconds, and configured health clients use `api.health-timeout`,
also defaulting to 2 seconds. Retries, multiple calls and batch processing can extend total duration.

### Development Swagger documentation

These links use the downstream hosts configured in [dev Helm values](../helm_deploy/values-dev.yaml).
Access may require the appropriate MoJ network or VPN.

| External API | Dev Swagger UI |
| --- | --- |
| HMPPS Auth | [Swagger UI](https://sign-in-dev.hmpps.service.justice.gov.uk/auth/swagger-ui/index.html#/) |
| Prison API | [Swagger UI](https://prison-api-dev.prison.service.justice.gov.uk/swagger-ui/index.html#/) |
| Prisoner Search API | [Swagger UI](https://prisoner-search-dev.prison.service.justice.gov.uk/swagger-ui/index.html#/) |
| Non-Associations API | [Swagger UI](https://non-associations-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html#/) |
| Incentives API | [Swagger UI](https://incentives-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html#/) |
| Locations Inside Prison API | [Swagger UI](https://locations-inside-prison-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html#/) |
| NOMIS Mapping API | [Swagger UI](https://nomis-sync-prisoner-mapping-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html#/) |
| Case Notes API | [Swagger UI](https://dev.offender-case-notes.service.justice.gov.uk/swagger-ui/index.html#/) |
| Manage Adjudications API | [Swagger UI](https://manage-adjudications-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html#/) |
| External Movements API | [Swagger UI](https://external-movements-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html#/) |
| GOV.UK bank holidays | No separate dev Swagger UI; [public JSON feed](https://www.gov.uk/bank-holidays.json) |

### HMPPS Auth

Relative to `HMPPS_AUTH_URL` (which includes `/auth` in deployment values):

| Method and path | Purpose |
| --- | --- |
| `POST /oauth/token` | Acquire client-credentials tokens through Spring Security's OAuth2 client manager |
| `GET /.well-known/jwks.json` | Resolve signing keys for validation of incoming API JWTs |

These infrastructure calls are configured in
[`application.yml`](../src/main/resources/application.yml), not a hand-written Auth client.
Do not confuse incoming JWT validation with obtaining this API's own downstream tokens.
There is no direct Token Verification API client in this backend.

## Endpoint inventory

Paths are relative to the configured downstream base URL. `POST` does not necessarily mean a
remote mutation: bulk searches commonly use a JSON request body. **Case-note creation is the
business-data write in this inventory**; the other business calls retrieve data.

### Prison API

[Client implementation](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/client/prisonapi/api/PrisonApiClient.kt).

| Method and path | Request / response and use |
| --- | --- |
| `GET /api/bookings/offenderNo/{prisonerNumber}` | Minimal prisoner details, including booking identity; used during offender-merge handling |
| `POST /api/schedules/{prisonCode}/appointments` | Prisoner-number set in body; optional `date`, `timeSlot`; NOMIS appointments for scheduled-event aggregation before appointment rollout |
| `POST /api/schedules/{prisonCode}/activities` | Same filters / body pattern; NOMIS activities before activity rollout |
| `POST /api/schedules/{prisonCode}/courtEvents` | Same filters / body pattern; court events in aggregated schedules |
| `POST /api/schedules/{prisonCode}/visits` | Same filters / body pattern; visits in aggregated schedules |
| `GET /api/schedules/{prisonCode}/locations/{locationId}/usage/VISIT` | Required `date`, optional `timeSlot`; visits occupying a numeric NOMIS location |
| `POST /api/schedules/{agencyId}/externalTransfers` | Prisoner-number set and required `date`; today's transfers in aggregated schedules |
| `GET /api/agencies/{agencyId}/locations/type/{type}` | Locations of a type; `LocationService` requests `CELL` for location-group filtering |
| `GET /api/reference-domains/domains/{domain}/codes/{code}` | Education level (`EDU_LEVEL`) and study area (`STUDY_AREA`) validation / descriptions |
| `POST /api/education/prisoners` | Prisoner-number list; education records for allocation suitability |
| `GET /api/agencies/{prisonCode}/eventLocations` | Location metadata for mapping aggregated scheduled events |
| `GET /api/agencies/{prisonCode}/eventLocationsBooked` | Required `bookedOnDay`, optional `timeSlot`; occupied event locations |
| `POST /api/movements/offenders?latestOnly=false` | Prisoner-number set; movement history, filtered locally to departures from the requested prison, for allocation expiry and appointment-attendee maintenance |

[`ScheduledEventService`](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/service/ScheduledEventService.kt)
selects the data source by rollout flags: activities and appointments come from the local database
once their respective capability is rolled out. This is **rollout selection, not an outage fallback**.
In this aggregation path, court-event requests are made for today or earlier; external-transfer
requests are made only for today. Transfer responses are redacted before returning them to callers.
Adjudication hearings come from Manage Adjudications, not Prison API.

Prisoner-number schedule calls skip the HTTP request for an empty set. A 404 from the
location-specific visits call is logged and converted to an empty list. Education results are
filtered for study area and education level, optionally excluding unfinished certifications,
then deduplicated.

### Prisoner Search API

[Client implementation](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/client/prisonersearchapi/api/PrisonerSearchApiClient.kt).

| Method and path | Request / response and use |
| --- | --- |
| `GET /prison/{prisonCode}/prisoners` | `size=2000`, `sort=lastName,firstName,asc`, optional `term`, and `responseFields` derived from the local `Prisoner` constructor; candidate search |
| `POST /prisoner-search/prisoner-numbers` | JSON `prisonerNumbers` list; bulk prisoner lookup for allocations, attendance, waiting lists, appointments and maintenance jobs |
| `GET /prisoner/{prisonerNumber}` | Single-prisoner lookup for validation, suitability and lifecycle-event handling |

The blocking bulk helper chunks requests into batches of 1000 by default and permits batch sizes
from 1 to 1000. The asynchronous bulk helper sends its supplied list in one request; it does not
apply that chunking. Both skip empty lists. The prison-list client requests a single page of up
to 2000, rather than automatically fetching all pages.

Responses provide prisoner identity, booking, location and status context to many workflows.
They enrich responses and drive decisions; they are not a replacement for the API's persisted
allocation / attendance / appointment records.

### Non-Associations API

[Client implementation](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/client/nonassociationsapi/api/NonAssociationsApiClient.kt).

| Method and path | Request / response and use |
| --- | --- |
| `GET /prisoner/{prisonerNumber}/non-associations` | A prisoner's non-associations for candidate / suitability information |
| `POST /non-associations/involving?prisonId={prisonCode}` | Prisoner-number list in body; conflicts involving candidates, allocations or waiting-list applicants |

The bulk operation skips empty lists. After configured retries, it logs failures and returns
`null`; this is distinct from an empty successful result and must not be interpreted as proof
that there are no non-associations. Other client operations generally propagate HTTP failures.

### Incentives API

[Client implementation](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/client/incentivesapi/api/IncentivesApiClient.kt).

| Method and path | Request / response and use |
| --- | --- |
| `GET /incentive/prison-levels/{prisonId}` | Prison incentive levels; migration uses them to validate and map activity pay rates |

`getIncentiveLevelsCached` uses the per-pod `prisonIncentiveLevels` cache, evicted on a one-day
schedule. There is also an uncached helper. No explicit `RetryApiService` retry operator is applied
in this client.

### Locations Inside Prison API

[Client implementation](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/client/locationsinsideprison/api/LocationsInsidePrisonAPIClient.kt).

| Method and path | Request / response and use |
| --- | --- |
| `GET /locations/{id}` | DPS UUID; `formatLocalName=true`; validate / enrich stored location references and support migration / repair |
| `GET /locations/prison/{prisonCode}/non-residential` | `formatLocalName=true`; non-residential locations for internal-location event views |
| `GET /locations/non-residential/prison/{prisonCode}/service/{serviceType}` | `formatLocalName=true`, `sortByLocalName=true`, `filterParents` (default false); service-appropriate appointment or activity locations |
| `GET /locations/prison/{prisonCode}/groups` | Location groups for the default location-group service |

Callers request `APPOINTMENT` and `PROGRAMMES_AND_ACTIVITIES` service types. Location-group
selection can instead use bundled regex configuration for configured prisons; not every group
lookup calls this API.

### NOMIS Mapping API

[Client implementation](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/client/nomismapping/api/NomisMappingAPIClient.kt).

| Method and path | Request / response and use |
| --- | --- |
| `GET /api/locations/dps/{id}` | DPS UUID to numeric NOMIS location ID |
| `GET /api/locations/nomis/{id}` | Numeric NOMIS location ID to DPS UUID |
| `POST /api/locations/dps` | Set of DPS UUIDs; bulk location mappings |
| `POST /api/locations/nomis` | Set of numeric NOMIS location IDs; bulk location mappings |

These are location mapping reads, not creation of activity / appointment synchronisation mappings.
They support location enrichment, appointment creation and migration, hearing location conversion,
and location repair / remap. Bulk helpers skip empty sets.

Single-item methods have nullable return types but assert a response body with `block()!!`;
the client does not turn a missing mapping or HTTP 404 into a reliable `null` result. Some repair
services catch mapping failures, warn and continue; other callers require the mapping to exist.

### Case Notes API

[Client implementation](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/client/casenotesapi/api/CaseNotesApiClient.kt).

| Method and path | Request / response and use |
| --- | --- |
| `POST /case-notes/{offenderNo}` | Create a case note with prison, type, subtype and text; used by attendance, deallocation and suspension workflows |
| `GET /case-notes/{offenderNo}/{dpsCaseNoteId}` | Read by UUID; enrich candidate allocation history and attendance synchronisation details |
| `GET /case-notes/{offenderNo}/{caseNoteId}` | Read by legacy numeric ID; recover the UUID during case-note reference migration |

All calls set `CaseloadId: ***`. Creation additionally sends a `Username` header from
`SecurityUtils.getUserNameForLoggedInUser()`, separate from the system-token authentication.
Without an authentication context, that helper supplies the existing operational fallback
`activities-management-admin-1`; do not assume every background call is attributed to a human.

The local type / subtype vocabulary is `GEN` / `NEG` and `HIS` / `NEG_GEN` / `IEP_WARN`.
An optional prefix is joined to the text and truncated to 4000 characters in the prefixed path.
The legacy-ID lookup maps a downstream 404 to `CaseNoteNotFoundException`. No explicit
`RetryApiService` retry operator is applied to reads or creation; do not assume failed creation
is automatically retried or safe to replay without checking whether the note was created.

### Manage Adjudications API

[Facade implementation](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/client/adjudications/ManageAdjudicationsApiFacade.kt)
and [hearing adapter](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/client/adjudications/AdjudicationsHearingAdapter.kt).

| Method and path | Request / response and use |
| --- | --- |
| `POST /reported-adjudications/hearings/{agencyId}` | `startDate`, `endDate` and prisoner-number list body; hearings for prisoner scheduled-event aggregation |
| `GET /reported-adjudications/hearings` | `hearingDate` query and `Active-Caseload` header; hearings for location-based event views |

The adapter filters by prison regime time slot and resolves each hearing's DPS location UUID
through NOMIS Mapping to produce the numeric-location projection expected by the scheduling models.
A hearing aggregation can therefore depend on both services, with additional mapping calls per hearing.

### External Movements API

[Client implementation](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/client/externalmovementsapi/api/ExternalMovementsApiClient.kt).

| Method and path | Request / response and use |
| --- | --- |
| `POST /search/prisons/{prisonCode}/external-activities` | JSON `personIdentifiers`, `start` and `end`; external activities within a date-time range |

The prisoner-specific scheduling path includes these activities when external activities are
rolled out and `includeExternalMovements` is requested. The location-event path also calls this
client for a prison / date-time range without restricting the request to a prisoner list.
An empty `personIdentifiers` list is sent in that path; unlike several other bulk clients,
this client does not short-circuit an empty list.

### GOV.UK bank holidays

[Client implementation](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/client/bankholidayapi/api/BankHolidayApiClient.kt).

| Method and path | Request / response and use |
| --- | --- |
| `GET /bank-holidays.json` | Public calendar; `BankHolidayService` uses England and Wales events when deciding whether activities run |

The default base URL is `https://www.gov.uk`, without the JSON path. No OAuth token or prisoner
data is required. Results use the per-pod `bankHolidays` cache, evicted on a seven-day schedule.
No explicit `RetryApiService` retry operator is applied.

## Retries, failures and health

[`RetryApiService`](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/client/RetryApiService.kt)
is explicitly used by Prison API, Prisoner Search, Non-Associations, Locations, NOMIS Mapping,
Adjudications and External Movements clients. Each supplies its own
`<api-prefix>.retry.max-retries` (default 2) and `<api-prefix>.retry.backoff-millis` (default 250).
For example, Prisoner Search uses `prisoner-search.api.retry.*`; Adjudications uses
`manage.adjudications.api.retry.*`.

Two retries mean up to three attempts, with Reactor's exponential backoff policy. The shared helper
also defines fallback `hmpps.web-client.*` settings, but these clients pass their explicit per-client
values. Retries are limited to `WebClientRequestException` (including an immediate cause of that type)
or HTTP **502 Bad Gateway**. They are not a blanket retry of every 4xx, 5xx or timeout.
Exhaustion propagates the final failure unless the specific operation catches it.

In REST requests, uncaught `WebClientResponseException` is handled by
[`ControllerAdvice`](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/config/ControllerAdvice.kt),
which preserves the downstream HTTP status and attempts to map its error body. Other uncaught
exceptions use the generic error handler unless a more specific handler applies. Background jobs
and queue listeners have their own failure handling; an upstream HTTP error does not imply a
rollback of already-committed work or previously created remote case notes.

[`HealthCheck.kt`](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/health/HealthCheck.kt)
registers shared-library `HealthPingCheck` components for all nine authenticated business APIs.
Their health WebClients are separate from the authorised business clients. An Auth health WebClient
bean is also configured. The bank-holiday client has no corresponding health component here.
A successful ping is not proof that business endpoint roles, mappings, data or writes will succeed.

## Data handling and operational implications

Outbound requests carry prisoner numbers, booking-related context, prisons, date ranges and location
identifiers. Case-note creation additionally sends free text and acting-user attribution. Responses
can include names, alerts, non-associations, education, movement history and case-note text.
Treat request / response logging and diagnostic captures accordingly.

Calls occur in staff requests, inbound-event processing, migrations and jobs. Scheduling can make
several downstream calls concurrently, and enrichment or hearing conversion can multiply calls.
Investigate the originating workflow as well as a client timeout when assessing latency or load.
Caching here is limited to the explicit Incentives and bank-holiday caches; it is not a general
cache of all downstream responses.

This backend has **no direct business client** for Alerts API, Book A Video Link API, Manage Users,
Frontend Components, Token Verification or Digital Prison Reporting HTTP endpoints. Those belong
to frontend configuration / integrations, not this outbound REST inventory.

## Maintenance sources

The client links above define exact paths, payloads, headers and local error handling.
[WebClient configuration](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/config/WebClientConfiguration.kt),
[OAuth2 configuration](../src/main/resources/application.yml),
[cache configuration](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/config/CacheConfiguration.kt)
and [environment values](../helm_deploy) define connection behaviour.
The [service implementations](../src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsactivitiesmanagementapi/service)
define when calls occur and how results affect business decisions. Update this document alongside
changes to those surfaces.

[Previous: Cron jobs](04-cron-jobs.md) | [Index](README.md)
