# Data flow diagram - Activities and appointments

These diagrams show data movement through the API and frontend, including the trust boundaries
relevant to threat modelling. They document interfaces and controls, not findings from a security
assessment. See the [high-level design](high-level-design.md) for component responsibilities.

## How to read the diagrams

Dashed boxes identify trust boundaries. Blue nodes are service-owned processes, yellow nodes are
stores or messaging infrastructure, and grey nodes are dependencies outside this service's ownership.
Boundaries are logical: workloads running in the same cluster do not automatically share an identity
or authorisation context. HTTPS ingress and internal HTTP job calls are distinguished explicitly.

Flows use stable prefixes: `C` for context, `U` for UI and `A` for API. Arrow labels describe the
request or publication; responses normally return along the same connection. No claim is made here
about external producers' authentication or infrastructure that is not configured in these repositories.

## Context

```mermaid
flowchart LR
  subgraph TB1["TB1 - Staff device"]
    staff(["Prison staff browser"])
  end
  subgraph TB2["TB2 - Service workloads on Cloud Platform"]
    ui["Activities UI"]
    api["Activities API"]
    cron["CronJobs"]
  end
  subgraph TB3["TB3 - Managed data and messaging"]
    redis[("Redis")]
    db[("PostgreSQL")]
    messaging(["SNS / SQS"])
    s3[("Analytical Platform S3")]
  end
  subgraph TB4["TB4 - Shared services and other consumers"]
    auth["HMPPS Auth / Token Verification"]
    shared["Shared HMPPS APIs"]
    consumers["NOMIS migration / sync,<br/>Integration API, other clients"]
  end
  subgraph TB5["TB5 - Public data provider"]
    govuk["GOV.UK bank holidays"]
  end

  staff <-->|"C1 HTTPS pages, forms, session cookie"| ui
  staff <-->|"C2 OAuth2 sign-in"| auth
  ui -->|"C3 token exchange / verification"| auth
  ui <-->|"C4 HTTPS, user or selected system JWT"| api
  ui <-->|"C5 sessions, journeys, tokens"| redis
  api <-->|"C6 SQL over TLS in deployment"| db
  ui <-->|"C7 authenticated API calls"| shared
  api <-->|"C8 client-credentials API calls"| shared
  api <-->|"C9 domain events, commands, jobs, audit"| messaging
  api -->|"C10 purposeful-activity CSV"| s3
  consumers <-->|"C11 HTTPS REST, bearer JWT"| api
  messaging -->|"C12 outbound domain events"| consumers
  ui & api -->|"C13 HTTPS calendar data"| govuk
  cron -->|"C14 internal HTTP /job/*"| api
  api -->|"C15 JWKS / client credentials"| auth

  classDef boundary fill:none,stroke:#333,stroke-width:2px,stroke-dasharray:6 4
  classDef process fill:#dae8fc,stroke:#6c8ebf,color:#000
  classDef store fill:#fff2cc,stroke:#d6b656,color:#000
  classDef external fill:#e8e8e8,stroke:#999,color:#000
  class TB1,TB2,TB3,TB4,TB5 boundary
  class ui,api,cron process
  class redis,db,messaging,s3 store
  class auth,shared,consumers,govuk external
```

`C9` is decomposed below: the queues do not all accept the same envelope or have the same producer.
`C11` shows authorised consumers of this API, not direct access from arbitrary external organisations.
Any external gateway's consumer-authentication scheme is outside this documentation's verified scope.

## UI layer

```mermaid
flowchart LR
  browser(["Browser"])
  ingress{{"UI TLS ingress<br/>allowlist"}}
  subgraph UI["Frontend application boundary"]
    middleware["Session, Passport, token verification,<br/>CSRF, current user, route roles"]
    journeys["Routes / journey services<br/>Nunjucks rendering"]
    clients["REST API clients"]
  end
  redis[("Redis<br/>session, journey and token stores")]
  auth["HMPPS Auth"]
  verification["Token Verification API"]
  api["Activities API"]
  shared["Shared HMPPS APIs<br/>including DPS components"]

  browser <-->|"U1 HTTPS request / response"| ingress
  ingress <--> middleware
  middleware <--> journeys
  journeys <--> clients
  middleware <-->|"U2 server-side session"| redis
  journeys <-->|"U3 journey state"| redis
  clients <-->|"U4 cached system tokens"| redis
  browser <-->|"U5 sign-in redirect / authorization code"| auth
  middleware -->|"U6 code exchange"| auth
  middleware -->|"U7 verify user token"| verification
  clients -->|"U8 client-credentials grant when needed"| auth
  clients <-->|"U9 user JWT + Caseload-Id for most calls;<br/>selected system-token lookups"| api
  clients <-->|"U10 downstream-specific user / system tokens"| shared

  classDef boundary fill:none,stroke:#333,stroke-width:2px,stroke-dasharray:6 4
  classDef process fill:#dae8fc,stroke:#6c8ebf,color:#000
  classDef store fill:#fff2cc,stroke:#d6b656,color:#000
  classDef external fill:#e8e8e8,stroke:#999,color:#000
  class UI boundary
  class middleware,journeys,clients process
  class redis store
  class auth,verification,api,shared external
```

The browser posts form data, including prisoner selections and free text, to the UI rather than
calling the Activities API directly in the normal staff journey. Session and journey stores can
contain prisoner information as well as staff identity and bearer tokens. Redis is not just a
cache of non-sensitive reference data.

The UI reads active-caseload information through the DPS components integration. Route-specific
`ROLE_ACTIVITY_HUB` checks protect selected activity-management journeys, and rollout middleware
controls feature availability. These UI checks complement, rather than replace, API authorisation.
The frontend has a Digital Prison Reporting API URL configured, but no reporting route is wired
in the inspected route composition, so no active reporting flow is drawn here. This configuration
is not a UI connection to the activities database.

## API layer

```mermaid
flowchart TB
  caller["UI / authorised REST clients"]
  ingress{{"API TLS ingress<br/>allowlist and restricted paths"}}
  cron["Internal CronJobs"]
  auth["HMPPS Auth"]
  subgraph API["API application boundary"]
    resources["REST resources<br/>JWT and endpoint roles"]
    triggers["Ingress-protected job / utility routes"]
    services["Services / domain rules<br/>operation-specific caseload checks"]
    listeners["Domain-event, external-command<br/>and internal-job listeners"]
    repositories["JPA repositories / SQL views"]
    publisher["Outbound domain-event / audit publishers"]
    reports["Purposeful-activity export"]
  end
  db[("PostgreSQL")]
  shared["Shared HMPPS APIs"]
  events(["activities SQS + DLQ"])
  commands(["updatefromexternalsystemevents SQS + DLQ"])
  jobs(["activitiesmanagementjobs SQS + DLQ"])
  sns(["domainevents SNS"])
  audit(["audit SQS"])
  s3[("Analytical Platform S3")]

  caller <-->|"A1 HTTPS + bearer JWT"| ingress
  ingress <--> resources
  cron -->|"A2 internal HTTP; no user JWT"| triggers
  resources --> services
  triggers --> services
  resources -->|"A3 JWT signing keys"| auth
  services <-->|"A4 client credentials / authenticated calls"| shared
  services --> repositories
  repositories <-->|"A5 domain state, history, read models"| db
  events -->|"A6 SNS envelope / prisoner domain event"| listeners
  commands -->|"A7 command envelope, actor in who"| listeners
  services -->|"A8 jobId / eventType / attributes"| jobs
  jobs -->|"A9 internal work"| listeners
  listeners --> services
  services --> publisher
  publisher -->|"A10 enabled domain events"| sns
  publisher -->|"A11 HMPPS audit messages"| audit
  services --> reports
  reports --> repositories
  reports -->|"A12 temporary CSV upload"| s3

  classDef boundary fill:none,stroke:#333,stroke-width:2px,stroke-dasharray:6 4
  classDef process fill:#dae8fc,stroke:#6c8ebf,color:#000
  classDef store fill:#fff2cc,stroke:#d6b656,color:#000
  classDef external fill:#e8e8e8,stroke:#999,color:#000
  class API boundary
  class resources,triggers,services,listeners,repositories,publisher,reports process
  class db,events,commands,jobs,sns,audit,s3 store
  class caller,cron,auth,shared external
```

The diagram omits in-process calls that do not cross a boundary. API outbound HTTP clients acquire
client-credentials tokens from HMPPS Auth; the GOV.UK bank-holiday request is unauthenticated.
Queue traffic is governed by AWS permissions rather than REST bearer-token checks.

## Trust boundaries and significant data

| Crossing | Data / identity | Control or dependency |
| --- | --- | --- |
| Browser to UI (`C1`, `U1`) | Session cookie, form submissions, rendered prisoner / staff information | TLS ingress and allowlist; sessions, CSRF and web-security middleware |
| Sign-in and verification (`C2-C3`, `U5-U8`) | Authorization code, state, user token, client credentials | HMPPS Auth, Passport OAuth2 state, server-side token handling and Token Verification |
| UI / other clients to API (`C4`, `C11`, `U9`, `A1`) | JWT, prison context, prisoner identifiers and domain data | JWT validation and endpoint roles; caseload comparison where invoked |
| UI to Redis (`C5`, `U2-U4`) | Sessions, journeys and tokens | Redis credentials and configured TLS; independent of API database controls |
| API to PostgreSQL (`C6`, `A5`) | Operational prisoner data, staff usernames, comments, history | Database credentials and deployed `verify-full` TLS; Flyway-managed schema |
| Calls to shared APIs (`C7-C8`, `U10`, `A4`) | Prisoner / booking / location identifiers and enriched responses | Per-service token choice and downstream authorisation |
| Inbound queues (`A6-A9`) | Prisoner events, actor-attributed commands and internal work | AWS producer / consumer permissions, envelope parsing, handlers, validation and DLQs |
| Outbound messaging (`A10-A11`) | Resource IDs, booking IDs, category codes, audit actor and details | AWS permissions and publication switches; separate domain and audit contracts |
| Reporting (`C10`, `A12`) | Operational CSV exports | S3 permissions and upload encryption configuration; database reporting access is separately configured |
| Internal job routes (`C14`, `A2`) | Job triggers and maintenance parameters | Ingress denies restricted paths; application JWT authentication deliberately bypassed |
| Public calendar (`C13`) | Bank-holiday data | HTTPS public endpoint; no prisoner data required |

Prisoner data is both persisted and transiently enriched here. Free text, Redis journey data, reports,
audit details, telemetry and diagnostic logs must be included when assessing data exposure, not just
REST responses and primary tables.

## Authentication and token flow

```mermaid
sequenceDiagram
  participant Browser
  participant UI as Activities UI
  participant Auth as HMPPS Auth
  participant Verify as Token Verification API
  participant Redis
  participant API as Activities API
  participant Downstream as Shared HMPPS API

  Browser->>UI: Request protected page
  UI-->>Browser: Redirect to authorization endpoint (state)
  Browser->>Auth: Staff sign-in
  Auth-->>Browser: Redirect to UI callback with code
  Browser->>UI: Callback code and state
  UI->>Auth: Exchange code using UI OAuth client
  Auth-->>UI: User access token and user context
  UI->>Redis: Store server-side session
  UI-->>Browser: Session cookie
  Browser->>UI: Authenticated page request
  UI->>Verify: Verify session's user token
  alt Most Activities API calls
    UI->>API: User JWT, usually with Caseload-Id
  else Rollout lookups / getAttendanceDetails
    UI->>Auth: Client credentials (unless token cached)
    Auth-->>UI: System JWT
    UI->>API: System JWT
  end
  API->>Auth: Resolve signing keys via JWKS as needed
  Note over API: Check endpoint roles and applicable caseload rules
  API->>Auth: API client credentials (when downstream token needed)
  Auth-->>API: API's downstream access token
  API->>Downstream: Authenticated request
  Downstream-->>API: Response
  API-->>UI: Domain response
  UI-->>Browser: Rendered HTML
```

This is a representative sequence, not a promise that every request exchanges or verifies tokens
over the network: session handling, key caching and token caching avoid some calls.
External-command attribution instead comes from `who`; it is not established by this browser flow.

## Sources and scope

The [high-level design sources](high-level-design.md#implementation-sources) link to API security,
caseload checking, frontend authentication and deployment configuration.
[Document 2](02-nomis-sync-and-migration.md) links the message contracts and listeners.
These diagrams do not assert the upstream Integration API gateway controls, exact deployed queue
policies, consumer inventory or NOMIS write implementation; those require the owning systems'
configuration.
