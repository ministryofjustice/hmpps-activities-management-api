package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDate
import java.time.LocalDateTime

class BulkAppointmentCreatedEvent(
  private val bulkAppointmentId: Long,
  private val prisonCode: String,
  private val categoryCode: String,
  private val hasDescription: Boolean,
  private val internalLocationId: Long?,
  private val startDate: LocalDate,
  private val prisonerNumbers: List<String>,
  createdAt: LocalDateTime,

) : AuditableEvent(
  auditType = AuditType.APPOINTMENT,
  auditEventType = AuditEventType.BULK_APPOINTMENT_CREATED,
  details = "A bulk appointment with ID '$bulkAppointmentId' with category $categoryCode and starting on $startDate " +
    "at prison $prisonCode was created",
  createdAt = createdAt,
),
  HmppsAuditable {

  override fun toJson(): String = generateHmppsAppointmentAuditJson(
    bulkAppointmentId = bulkAppointmentId,
    prisonCode = prisonCode,
    categoryCode = categoryCode,
    hasDescription = hasDescription,
    internalLocationId = internalLocationId,
    startDate = startDate,
    prisonerNumbers = prisonerNumbers,
    createdAt = createdAt,
    createdBy = createdBy,
  )
}
