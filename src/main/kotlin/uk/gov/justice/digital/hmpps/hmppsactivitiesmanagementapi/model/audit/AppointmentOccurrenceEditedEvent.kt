package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AppointmentOccurrenceEditedEvent(
  private val appointmentId: Long,
  private val appointmentOccurrenceId: Long,
  private val prisonCode: String,
  private val originalCategoryCode: String,
  private val categoryCode: String,
  private val originalInternalLocationId: String,
  private val internalLocationId: String,
  private val originalStartDate: LocalDate,
  private val startDate: LocalDate,
  private val originalStartTime: LocalTime,
  private val startTime: LocalTime,
  private val originalEndTime: LocalTime,
  private val endTime: LocalTime,
  // TODO Extra Information?
  private val applyTo: ApplyTo?,
  createdAt: LocalDateTime,

) : AuditableEvent(
  auditType = AuditType.APPOINTMENT,
  auditEventType = AuditEventType.APPOINTMENT_OCCURRENCE_EDITED,
  details = "An appointment with ID '$appointmentId' with category $categoryCode and starting on $startDate " +
    "at prison $prisonCode was edited",
  createdAt = createdAt,
),
  HmppsAuditable {

  override fun toJson(): String = generateHmppsAppointmentAuditJson(
    appointmentId = appointmentId,
    appointmentOccurrenceId = appointmentOccurrenceId,
    prisonCode = prisonCode,
    originalCategoryCode = originalCategoryCode,
    categoryCode = categoryCode,
    originalInternalLocationId = originalInternalLocationId,
    internalLocationId = internalLocationId,
    originalStartDate = originalStartDate,
    startDate = startDate,
    originalStartTime = originalStartTime,
    startTime = startTime,
    originalEndTime = originalEndTime,
    endTime = endTime,
    applyTo = applyTo,
    createdAt = createdAt,
    createdBy = createdBy,
  )
}
