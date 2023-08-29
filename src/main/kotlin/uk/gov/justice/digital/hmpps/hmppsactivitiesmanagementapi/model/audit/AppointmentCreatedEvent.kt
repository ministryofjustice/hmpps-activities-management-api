package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Repeat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AppointmentCreatedEvent(
  private val appointmentId: Long,
  private val prisonCode: String,
  private val categoryCode: String,
  private val hasDescription: Boolean,
  private val internalLocationId: String,
  private val startDate: LocalDate,
  private val startTime: LocalTime,
  private val endTime: LocalTime,
  private val isRepeat: Boolean,
  private val repeatPeriod: Repeat.RepeatPeriod?,
  private val repeatCount: Int,
  private val hasExtraInformation: Boolean,
  private val prisonerNumbers: Set<String>,
  createdAt: LocalDateTime,

) : AuditableEvent(
  auditType = AuditType.APPOINTMENT,
  auditEventType = AuditEventType.APPOINTMENT_CREATED,
  details = "An appointment with ID '$appointmentId' with category $categoryCode and starting on $startDate " +
    "at prison $prisonCode was created",
  createdAt = createdAt,
),
  HmppsAuditable {

  override fun toJson(): String = generateHmppsAppointmentAuditJson(
    appointmentId = appointmentId,
    prisonCode = prisonCode,
    categoryCode = categoryCode,
    hasDescription = hasDescription,
    internalLocationId = internalLocationId,
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    isRepeat = isRepeat,
    repeatPeriod = repeatPeriod,
    repeatCount = repeatCount,
    hasExtraInformation = hasExtraInformation,
    prisonerNumbers = prisonerNumbers,
    createdAt = createdAt,
    createdBy = createdBy,
  )
}
