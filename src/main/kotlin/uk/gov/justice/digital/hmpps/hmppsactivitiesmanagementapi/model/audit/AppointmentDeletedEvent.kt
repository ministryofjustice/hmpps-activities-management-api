package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import java.time.LocalDateTime

class AppointmentDeletedEvent(
  private val appointmentId: Long,
  private val appointmentOccurrenceId: Long,
  private val prisonCode: String,
  private val applyTo: ApplyTo?,
  createdAt: LocalDateTime,

) : AuditableEvent(
  auditType = AuditType.APPOINTMENT,
  auditEventType = AuditEventType.APPOINTMENT_DELETED,
  details = "An appointment with ID '$appointmentId' and Occurrence ID '$appointmentOccurrenceId' " +
    "at prison $prisonCode was deleted",
  createdAt = createdAt,
),
  HmppsAuditable {

  override fun toJson(): String = generateHmppsAppointmentAuditJson(
    appointmentId = appointmentId,
    appointmentOccurrenceId = appointmentOccurrenceId,
    prisonCode = prisonCode,
    applyTo = applyTo,
    createdAt = createdAt,
    createdBy = createdBy,
  )
}
