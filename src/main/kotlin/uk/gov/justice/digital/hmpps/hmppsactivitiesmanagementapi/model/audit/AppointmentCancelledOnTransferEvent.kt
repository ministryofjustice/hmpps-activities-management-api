package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDateTime

class AppointmentCancelledOnTransferEvent(
  private val appointmentSeriesId: Long,
  private val appointmentId: Long,
  private val prisonCode: String,
  private val prisonerNumber: String,
  createdAt: LocalDateTime,
) : AuditableEvent(
  auditType = AuditType.APPOINTMENT,
  auditEventType = AuditEventType.APPOINTMENT_CANCELLED_ON_TRANSFER,
  details = "An appointment with id '$appointmentId' from series id '$appointmentSeriesId' " +
    "at prison $prisonCode was cancelled on transfer of prisoner $prisonerNumber",
  createdTime = createdAt,
  createdBy = "cancelled-on-transfer-event",

),
  HmppsAuditable {

  override fun toJson(): String = generateHmppsAppointmentAuditJson(
    appointmentSeriesId = appointmentSeriesId,
    appointmentId = appointmentId,
    prisonCode = prisonCode,
    prisonerNumbers = listOf(prisonerNumber),
    createdTime = createdTime,
    createdBy = createdBy,
  )
}
