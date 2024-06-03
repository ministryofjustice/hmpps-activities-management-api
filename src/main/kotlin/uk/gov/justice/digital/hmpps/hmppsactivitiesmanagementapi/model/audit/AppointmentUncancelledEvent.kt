package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import java.time.LocalDateTime

class AppointmentUncancelledEvent(
  val appointmentSeriesId: Long,
  val appointmentId: Long,
  val prisonCode: String,
  val applyTo: ApplyTo?,
  createdAt: LocalDateTime,
  createdBy: String,

) : AuditableEvent(
  auditType = AuditType.APPOINTMENT,
  auditEventType = AuditEventType.APPOINTMENT_UNCANCELLED,
  details = "An appointment with id '$appointmentId' from series id '$appointmentSeriesId' " +
    "at prison $prisonCode was uncancelled",
  createdAt = createdAt,
  createdBy = createdBy,
),
  HmppsAuditable {

  override fun toJson(): String = generateHmppsAppointmentAuditJson(
    appointmentSeriesId = appointmentSeriesId,
    appointmentId = appointmentId,
    prisonCode = prisonCode,
    applyTo = applyTo,
    createdTime = createdAt,
    createdBy = createdBy,
  )
}
