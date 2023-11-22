package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AppointmentEditedEvent(
  private val appointmentSeriesId: Long,
  private val appointmentId: Long,
  private val prisonCode: String,
  private val originalCategoryCode: String,
  private val categoryCode: String,
  private val originalTierCode: String?,
  private val tierCode: String?,
  private val originalOrganiserCode: String?,
  private val organiserCode: String?,
  private val originalInternalLocationId: Long?,
  private val internalLocationId: Long?,
  private val originalStartDate: LocalDate,
  private val startDate: LocalDate,
  private val originalStartTime: LocalTime,
  private val startTime: LocalTime,
  private val originalEndTime: LocalTime?,
  private val endTime: LocalTime?,
  // TODO Extra Information?
  private val applyTo: ApplyTo?,
  createdAt: LocalDateTime,

) : AuditableEvent(
  auditType = AuditType.APPOINTMENT,
  auditEventType = AuditEventType.APPOINTMENT_EDITED,
  details = "An appointment with id '$appointmentId' from series id '$appointmentSeriesId' with category $categoryCode " +
    "and starting on $startDate at prison $prisonCode was edited",
  createdAt = createdAt,
),
  HmppsAuditable {

  override fun toJson(): String = generateHmppsAppointmentAuditJson(
    appointmentSeriesId = appointmentSeriesId,
    appointmentId = appointmentId,
    prisonCode = prisonCode,
    originalCategoryCode = originalCategoryCode,
    categoryCode = categoryCode,
    originalTierCode = originalTierCode,
    tierCode = tierCode,
    originalOrganiserCode = originalOrganiserCode,
    organiserCode = organiserCode,
    originalInternalLocationId = originalInternalLocationId,
    internalLocationId = internalLocationId,
    originalStartDate = originalStartDate,
    startDate = startDate,
    originalStartTime = originalStartTime,
    startTime = startTime,
    originalEndTime = originalEndTime,
    endTime = endTime,
    applyTo = applyTo,
    createdTime = createdAt,
    createdBy = createdBy,
  )
}
