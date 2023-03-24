package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance as AppointmentInstanceModel

@Entity
@Table(name = "v_appointment_instance")
data class AppointmentInstance(
  @Id
  val appointmentInstanceId: Long,

  val appointmentId: Long,

  val appointmentOccurrenceId: Long,

  val appointmentOccurrenceAllocationId: Long,

  val categoryCode: String,

  val prisonCode: String,

  val internalLocationId: Long?,

  val inCell: Boolean,

  val prisonerNumber: String,

  val bookingId: Long,

  val appointmentDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime?,

  val comment: String? = null,

  val created: LocalDateTime = LocalDateTime.now(),

  val createdBy: String,

  val updated: LocalDateTime? = null,

  val updatedBy: String? = null,
) {
  fun toModel() = AppointmentInstanceModel(
    id = appointmentInstanceId,
    appointmentId = appointmentId,
    appointmentOccurrenceId = appointmentOccurrenceId,
    appointmentOccurrenceAllocationId = appointmentOccurrenceAllocationId,
    categoryCode = categoryCode,
    prisonCode = prisonCode,
    internalLocationId = internalLocationId,
    inCell = inCell,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    appointmentDate = appointmentDate,
    startTime = startTime,
    endTime = endTime,
    comment = comment,
    created = created,
    createdBy = createdBy,
    updated = updated,
    updatedBy = updatedBy,
  )
}

fun List<AppointmentInstance>.toModel() = map { it.toModel() }
