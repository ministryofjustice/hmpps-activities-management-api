package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Column
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

  @Column(name = "appointment_series_id")
  val appointmentId: Long,

  @Column(name = "appointment_id")
  val appointmentOccurrenceId: Long,

  @Column(name = "appointment_attendee_id")
  val appointmentOccurrenceAllocationId: Long,

  @Enumerated(EnumType.STRING)
  val appointmentType: AppointmentType,

  val prisonCode: String,

  val prisonerNumber: String,

  val bookingId: Long,

  val categoryCode: String,

  @Column(name = "custom_name")
  val appointmentDescription: String?,

  val internalLocationId: Long?,

  val inCell: Boolean,

  val appointmentDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime?,

  @Column(name = "extra_information")
  val comment: String?,

  @Column(name = "created_time")
  val created: LocalDateTime = LocalDateTime.now(),

  val createdBy: String,

  @Column(name = "updated_time")
  val updated: LocalDateTime?,

  val updatedBy: String?,

  val isCancelled: Boolean,
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
