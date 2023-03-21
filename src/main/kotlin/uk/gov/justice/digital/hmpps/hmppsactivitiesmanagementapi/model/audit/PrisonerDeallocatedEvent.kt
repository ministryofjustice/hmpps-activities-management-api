package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import net.minidev.json.JSONObject
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.model.HmppsAuditEvent

class PrisonerDeallocatedEvent(
  private val activityId: Long,
  private val prisonerNumber: String,
  private val scheduleId: Long,

) : HmppsAuditable {
  override fun toAuditEvent(): HmppsAuditEvent {
    return HmppsAuditEvent(
      what = AuditEventType.PRISONER_DEALLOCATED.name,
      details = JSONObject(
        mapOf(
          "activityId" to activityId,
          "prisonerNumber" to prisonerNumber,
          "scheduleId" to scheduleId,
        ),
      ).toString(),
    )
  }
}
