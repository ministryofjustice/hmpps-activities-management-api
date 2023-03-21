package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.model.HmppsAuditEvent

interface HmppsAuditable : AuditableEvent {

  fun toAuditEvent(): HmppsAuditEvent
}
