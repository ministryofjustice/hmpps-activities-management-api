package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import net.minidev.json.JSONObject
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.model.HmppsAuditEvent

class PrisonerAcceptedFromWaitingListEvent(
  private val activityId: Long,
  private val prisonerNumber: String,

) : HmppsAuditable {
  override fun toAuditEvent(): HmppsAuditEvent {
    return HmppsAuditEvent(
      what = AuditEventType.PRISONER_ACCEPTED_FROM_WAITING_LIST.name,
      details = JSONObject(
        mapOf(
          "activityId" to activityId,
          "prisonerNumber" to prisonerNumber,
        ),
      ).toString(),
    )
  }
}
