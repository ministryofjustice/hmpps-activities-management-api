package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import net.minidev.json.JSONObject
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.model.HmppsAuditEvent

class IncentiveLevelWarningGivenForActivityAttendanceEvent(
  private val activityId: Long,
  private val prisonerNumber: String,
  private val scheduleId: Long,

) : HmppsAuditable {
  override fun toAuditEvent(): HmppsAuditEvent {
    return HmppsAuditEvent(
      what = AuditEventType.INCENTIVE_LEVEL_WARNING_GIVEN_FOR_ACTIVITY_ATTENDANCE.name,
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
