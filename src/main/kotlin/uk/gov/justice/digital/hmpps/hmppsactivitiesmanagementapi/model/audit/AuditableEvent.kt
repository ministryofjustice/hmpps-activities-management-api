package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import net.minidev.json.JSONObject
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Repeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
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

  fun generateHmppsActivityAuditJson(
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
  ): String = JSONObject.toJSONString(
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

  fun generateHmppsAppointmentAuditJson(
    appointmentId: Long? = null,
    bulkAppointmentId: Long? = null,
    appointmentOccurrenceId: Long? = null,
    prisonCode: String? = null,
    originalCategoryCode: String? = null,
    categoryCode: String? = null,
    hasDescription: Boolean? = null,
    originalInternalLocationId: String? = null,
    internalLocationId: String? = null,
    originalStartDate: LocalDate? = null,
    startDate: LocalDate? = null,
    originalStartTime: LocalTime? = null,
    startTime: LocalTime? = null,
    originalEndTime: LocalTime? = null,
    endTime: LocalTime? = null,
    isRepeat: Boolean? = null,
    repeatPeriod: Repeat.RepeatPeriod? = null,
    repeatCount: Int? = null,
    hasExtraInformation: Boolean? = null,
    prisonerNumbers: Set<String>? = null,
    applyTo: ApplyTo? = null,
    createdAt: LocalDateTime? = null,
    createdBy: String? = null,
  ): String = JSONObject.toJSONString(
    buildMap<String, Any> {
      appointmentId?.let { put("appointmentId", it) }
      bulkAppointmentId?.let { put("bulkAppointmentId", it) }
      appointmentOccurrenceId?.let { put("appointmentOccurrenceId", it) }
      prisonCode?.let { put("prisonCode", it) }
      originalCategoryCode?.let { put("originalCategoryCode", it) }
      categoryCode?.let { put("categoryCode", it) }
      hasDescription?.let { put("hasDescription", it) }
      originalInternalLocationId?.let { put("originalInternalLocationId", it) }
      internalLocationId?.let { put("internalLocationId", it) }
      originalStartDate?.let { put("originalStartDate", it.toIsoDate()) }
      startDate?.let { put("startDate", it.toIsoDate()) }
      originalStartTime?.let { put("originalStartTime", it.toIsoTime()) }
      startTime?.let { put("startTime", it.toIsoTime()) }
      originalEndTime?.let { put("originalEndTime", it.toIsoTime()) }
      endTime?.let { put("endTime", it.toIsoTime()) }
      isRepeat?.let { put("isRepeat", it) }
      repeatPeriod?.let { put("repeatPeriod", it) }
      repeatCount?.let { put("repeatCount", it) }
      hasExtraInformation?.let { put("hasExtraInformation", it) }
      prisonerNumbers?.let { put("prisonerNumbers", it) }
      applyTo?.let { put("applyTo", it) }
      createdAt?.let { put("createdAt", it.toIsoDateTime()) }
      createdBy?.let { put("createdBy", it) }
    },
  )
}
