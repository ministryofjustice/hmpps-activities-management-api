package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class AppointmentSeriesCreatedEvent(
  private val appointmentSeriesId: Long,
  private val prisonCode: String,
  private val categoryCode: String,
  private val hasCustomName: Boolean,
  private val internalLocationId: Long?,
  private val dpsLocationId: UUID?,
  private val startDate: LocalDate,
  private val startTime: LocalTime,
  private val endTime: LocalTime?,
  private val isRepeat: Boolean,
  private val frequency: AppointmentFrequency?,
  private val numberOfAppointments: Int?,
  private val hasExtraInformation: Boolean,
  private val prisonerNumbers: List<String>,
  createdTime: LocalDateTime,
  createdBy: String,
) : AuditableEvent(
  auditType = AuditType.APPOINTMENT,
  auditEventType = AuditEventType.APPOINTMENT_SERIES_CREATED,
  details = "An appointment series with id '$appointmentSeriesId' category $categoryCode and starting on $startDate " +
    "at prison $prisonCode was created",
  createdAt = createdTime,
  createdBy = createdBy,
),
  HmppsAuditable {

  override fun toJson(): String = generateHmppsAppointmentAuditJson(
    appointmentSeriesId = appointmentSeriesId,
    prisonCode = prisonCode,
    categoryCode = categoryCode,
    hasCustomName = hasCustomName,
    internalLocationId = internalLocationId,
    dpsLocationId = dpsLocationId,
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    isRepeat = isRepeat,
    frequency = frequency,
    numberOfAppointments = numberOfAppointments,
    hasExtraInformation = hasExtraInformation,
    prisonerNumbers = prisonerNumbers,
    createdTime = createdAt,
    createdBy = createdBy,
  )
}
