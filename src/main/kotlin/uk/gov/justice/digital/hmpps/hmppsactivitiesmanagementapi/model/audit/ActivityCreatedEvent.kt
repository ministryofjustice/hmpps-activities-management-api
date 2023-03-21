package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import net.minidev.json.JSONObject
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.model.HmppsAuditEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ActivityCreatedEvent(
  private val activityId: Long,
  private val prisonCode: String,
  private val categoryCode: String,
  private val summary: String,
  private val startDate: LocalDate,

) : HmppsAuditable {
  override fun toAuditEvent(): HmppsAuditEvent {
    return HmppsAuditEvent(
      what = AuditEventType.ACTIVITY_CREATED.name,
      details = JSONObject(
        mapOf(
          "activityId" to activityId,
          "prisonCode" to prisonCode,
          "categoryCode" to categoryCode,
          "summary" to summary,
          "startDate" to startDate.format(DateTimeFormatter.ISO_DATE_TIME),
        ),
      ).toString(),
    )
  }
}
