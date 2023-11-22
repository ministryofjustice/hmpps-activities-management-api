package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import net.minidev.json.JSONObject
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency
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
    appointmentSeriesId: Long? = null,
    appointmentSetId: Long? = null,
    appointmentId: Long? = null,
    prisonCode: String? = null,
    originalCategoryCode: String? = null,
    categoryCode: String? = null,
    originalTierCode: String? = null,
    tierCode: String? = null,
    originalOrganiserCode: String? = null,
    organiserCode: String? = null,
    hasCustomName: Boolean? = null,
    originalInternalLocationId: Long? = null,
    internalLocationId: Long? = null,
    originalStartDate: LocalDate? = null,
    startDate: LocalDate? = null,
    originalStartTime: LocalTime? = null,
    startTime: LocalTime? = null,
    originalEndTime: LocalTime? = null,
    endTime: LocalTime? = null,
    isRepeat: Boolean? = null,
    frequency: AppointmentFrequency? = null,
    numberOfAppointments: Int? = null,
    hasExtraInformation: Boolean? = null,
    prisonerNumbers: List<String>? = null,
    applyTo: ApplyTo? = null,
    createdTime: LocalDateTime? = null,
    createdBy: String? = null,
  ): String = JSONObject.toJSONString(
    buildMap<String, Any> {
      appointmentSeriesId?.let { put("appointmentSeriesId", it) }
      appointmentSetId?.let { put("appointmentSetId", it) }
      appointmentId?.let { put("appointmentId", it) }
      prisonCode?.let { put("prisonCode", it) }
      originalCategoryCode?.let { put("originalCategoryCode", it) }
      categoryCode?.let { put("categoryCode", it) }
      originalTierCode?.let { put("originalTierCode", it) }
      tierCode?.let { put("tierCode", it) }
      originalOrganiserCode?.let { put("originalOrganiserCode", it) }
      organiserCode?.let { put("organiserCode", it) }
      hasCustomName?.let { put("hasCustomName", it) }
      originalInternalLocationId?.let { put("originalInternalLocationId", it) }
      internalLocationId?.let { put("internalLocationId", it) }
      originalStartDate?.let { put("originalStartDate", it.toIsoDate()) }
      startDate?.let { put("startDate", it.toIsoDate()) }
      originalStartTime?.let { put("originalStartTime", it.toIsoTime()) }
      startTime?.let { put("startTime", it.toIsoTime()) }
      originalEndTime?.let { put("originalEndTime", it.toIsoTime()) }
      endTime?.let { put("endTime", it.toIsoTime()) }
      isRepeat?.let { put("isRepeat", it) }
      frequency?.let { put("frequency", it) }
      numberOfAppointments?.let { put("numberOfAppointments", it) }
      hasExtraInformation?.let { put("hasExtraInformation", it) }
      prisonerNumbers?.let { put("prisonerNumbers", it) }
      applyTo?.let { put("applyTo", it) }
      createdTime?.let { put("createdTime", it.toIsoDateTime()) }
      createdBy?.let { put("createdBy", it) }
    },
  )
}
