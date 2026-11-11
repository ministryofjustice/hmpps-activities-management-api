# Activities and appointments documentation

Architecture documentation for the Activities Management API and its
[frontend](https://github.com/ministryofjustice/hmpps-activities-management).

The structure follows the [Locations Inside Prison API documentation](https://github.com/ministryofjustice/hmpps-locations-inside-prison-api/tree/main/docs),
but the content describes this service's checked-in implementation. In particular, authentication,
prisoner data storage, event contracts and migration behaviour differ from the Locations service.
Deployment values describe configuration in source control, not a verification of live infrastructure.

## Reading guide

| Document | Purpose |
| --- | --- |
| [High-level design](high-level-design.md) | Components, responsibilities, dependencies, authentication, data and deployment |
| [Data flow diagram](data-flow-diagram.md) | Context, UI and API flows, trust boundaries and token sequence |
| [1. Architecture overview](01-architecture.md) | Current implementation and its architectural consequences |
| [2. NOMIS synchronisation and migration](02-nomis-sync-and-migration.md) | Rollout, migration endpoints, outbound events and reconciliation |
| [3. Database design](03-activities-db-design.md) | Activities, appointments, history and read models |
| [4. Cron jobs](04-cron-jobs.md) | Schedules, environment overrides, triggers, processing and completion checks |
| [5. External API calls](05-external-api-calls.md) | Backend outbound endpoints, usage, authentication, timeouts, retries and failure handling |

Diagrams are maintained as Mermaid in Markdown so their source can be reviewed alongside code.
The database diagrams are intentionally selective, not a complete physical schema.

## Status and maintenance

The numbered documents are **descriptions of the implementation**, not evidence that a historical
architecture decision was formally approved.

Update the relevant documents when changing authentication, API consumers, queues or event payloads,
data ownership, migration behaviour, or deployment boundaries. Each document links to implementation
sources; those sources remain authoritative for exact endpoint contracts and configuration.

Developer setup, build commands and operational procedures remain in the [project README](../README.md)
and [Helm documentation](../helm_deploy/README.md). In particular, the temporary location-remap runbook
is not replaced by these architecture documents.
