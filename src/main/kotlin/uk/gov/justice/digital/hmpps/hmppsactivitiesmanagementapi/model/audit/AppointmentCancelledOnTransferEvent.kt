package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDateTime

class AppointmentCancelledOnTransferEvent(
  private val appointmentId: Long,
  private val appointmentOccurrenceId: Long,
  private val prisonCode: String,
  private val prisonerNumber: String,
) : AuditableEvent(
  auditType = AuditType.APPOINTMENT,
  auditEventType = AuditEventType.APPOINTMENT_CANCELLED_ON_TRANSFER,
  details = "An appointment with ID '$appointmentId' and Occurrence ID '$appointmentOccurrenceId' " +
    "at prison $prisonCode was cancelled on transfer of prisoner $prisonerNumber",
  createdAt = LocalDateTime.now(),
  createdBy = "cancelled-on-transfer-event",
),
  HmppsAuditable {

  override fun toJson(): String = generateHmppsAppointmentAuditJson(
    appointmentId = appointmentId,
    appointmentOccurrenceId = appointmentOccurrenceId,
    prisonCode = prisonCode,
    prisonerNumbers = listOf(prisonerNumber),
    createdAt = createdAt,
    createdBy = createdBy,
  )
}
