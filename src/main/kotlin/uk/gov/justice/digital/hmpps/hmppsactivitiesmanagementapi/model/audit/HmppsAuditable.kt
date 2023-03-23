package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

interface HmppsAuditable {

  fun type(): AuditEventType
}
