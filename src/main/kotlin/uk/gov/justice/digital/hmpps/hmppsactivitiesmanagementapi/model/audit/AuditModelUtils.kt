package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import net.minidev.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
): String {
  val jsonMap = mutableMapOf<String, Any>()
  activityId?.let { jsonMap["activityId"] = it }
  activityName?.let { jsonMap["activityName"] = it }
  prisonCode?.let { jsonMap["prisonCode"] = it }
  prisonerNumber?.let { jsonMap["prisonerNumber"] = it }
  scheduleId?.let { jsonMap["scheduleId"] = it }
  date?.let { jsonMap["date"] = it.format(DateTimeFormatter.ISO_DATE) }
  startTime?.let { jsonMap["startTime"] = it.format(DateTimeFormatter.ISO_TIME) }
  endTime?.let { jsonMap["endTime"] = it.format(DateTimeFormatter.ISO_TIME) }
  createdAt?.let { jsonMap["createdAt"] = it.format(DateTimeFormatter.ISO_DATE_TIME) }
  createdBy?.let { jsonMap["createdBy"] = it }

  return JSONObject.toJSONString(jsonMap)
}
