package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import net.minidev.json.JSONObject
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.SecurityUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

abstract class AuditableEvent(

  val auditType: AuditType,
  val auditEventType: AuditEventType,
  val createdAt: LocalDateTime,
  private val details: String,
  val createdBy: String = SecurityUtils.getUserNameForLoggedInUser(),
) {
  override fun toString() = "$details. Event created on ${createdAt.toLocalDate()} " +
    "at ${createdAt.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} by $createdBy."

  fun generateHmppsAuditJson(
    activityId: Long? = null,
    activityName: String? = null,
    prisonCode: String? = null,
    prisonerNumber: String? = null,
    scheduleId: Long? = null,
    date: LocalDate? = null,
    startTime: LocalTime? = null,
    endTime: LocalTime? = null,
    createdAt: LocalDateTime? = null,
    createdBy: String? = null,
  ) = JSONObject.toJSONString(
    buildMap<String, Any> {
      activityId?.let { put("activityId", it) }
      activityName?.let { put("activityName", it) }
      prisonCode?.let { put("prisonCode", it) }
      prisonerNumber?.let { put("prisonerNumber", it) }
      scheduleId?.let { put("scheduleId", it) }
      date?.let { put("date", it.toIsoDate()) }
      startTime?.let { put("startTime", it.toIsoTime()) }
      endTime?.let { put("endTime", it.toIsoTime()) }
      createdAt?.let { put("createdAt", it.toIsoDateTime()) }
      createdBy?.let { put("createdBy", it) }
    },
  )
}
