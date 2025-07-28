package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AppointmentSetCreatedEvent(
  private val appointmentSetId: Long,
  private val prisonCode: String,
  private val categoryCode: String,
  private val hasCustomName: Boolean,
  private val internalLocationId: Long?,
  private val dpsLocationId: UUID?,
  private val startDate: LocalDate,
  private val prisonerNumbers: List<String>,
  createdAt: LocalDateTime,
  createdBy: String,
) : AuditableEvent(
  auditType = AuditType.APPOINTMENT,
  auditEventType = AuditEventType.APPOINTMENT_SET_CREATED,
  details = "An appointment set with id '$appointmentSetId' with category $categoryCode and starting on $startDate " +
    "at prison $prisonCode was created",
  createdAt = createdAt,
  createdBy = createdBy,
),
  HmppsAuditable {

  override fun toJson(): String = generateHmppsAppointmentAuditJson(
    appointmentSetId = appointmentSetId,
    prisonCode = prisonCode,
    categoryCode = categoryCode,
    hasCustomName = hasCustomName,
    internalLocationId = internalLocationId,
    dpsLocationId = dpsLocationId,
    startDate = startDate,
    prisonerNumbers = prisonerNumbers,
    createdTime = createdAt,
    createdBy = createdBy,
  )
}
