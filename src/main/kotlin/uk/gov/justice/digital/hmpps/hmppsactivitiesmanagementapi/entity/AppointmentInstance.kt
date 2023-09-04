package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance as AppointmentInstanceModel

@Entity
@Immutable
@Table(name = "v_appointment_instance")
data class AppointmentInstance(
  @Id
  val appointmentInstanceId: Long,

  val appointmentId: Long,

  val appointmentOccurrenceId: Long,

  val appointmentOccurrenceAllocationId: Long,

  @Enumerated(EnumType.STRING)
  val appointmentType: AppointmentType,

  val prisonCode: String,

  val prisonerNumber: String,

  val bookingId: Long,

  val categoryCode: String,

  val appointmentDescription: String?,

  val internalLocationId: Long?,

  val inCell: Boolean,

  val appointmentDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime?,

  val comment: String?,

  val created: LocalDateTime = LocalDateTime.now(),

  val createdBy: String,

  val isCancelled: Boolean,

  val updated: LocalDateTime?,

  val updatedBy: String?,
) {
  fun toModel() = AppointmentInstanceModel(
    id = appointmentInstanceId,
    appointmentId = appointmentId,
    appointmentOccurrenceId = appointmentOccurrenceId,
    appointmentOccurrenceAllocationId = appointmentOccurrenceAllocationId,
    appointmentType = appointmentType,
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    categoryCode = categoryCode,
    appointmentDescription = appointmentDescription,
    internalLocationId = internalLocationId,
    inCell = inCell,
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
