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

  val appointmentSeriesId: Long,

  val appointmentId: Long,

  val appointmentAttendeeId: Long,

  @Enumerated(EnumType.STRING)
  val appointmentType: AppointmentType,

  val prisonCode: String,

  val prisonerNumber: String,

  val bookingId: Long,

  val categoryCode: String,

  val customName: String?,

  val internalLocationId: Long?,

  val customLocation: String?,

  val inCell: Boolean,

  val onWing: Boolean,

  val offWing: Boolean,

  val appointmentDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime?,

  var unlockNotes: String?,

  val extraInformation: String?,

  val createdTime: LocalDateTime,

  val createdBy: String,

  val updatedTime: LocalDateTime?,

  val updatedBy: String?,

  val isCancelled: Boolean,
) {
  fun toModel() = AppointmentInstanceModel(
    id = appointmentInstanceId,
    appointmentSeriesId = appointmentSeriesId,
    appointmentId = appointmentId,
    appointmentAttendeeId = appointmentAttendeeId,
    appointmentType = appointmentType,
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    categoryCode = categoryCode,
    customName = customName,
    internalLocationId = internalLocationId,
    inCell = inCell,
    appointmentDate = appointmentDate,
    startTime = startTime,
    endTime = endTime,
    extraInformation = extraInformation,
    created = createdTime,
    createdBy = createdBy,
    updated = updatedTime,
    updatedBy = updatedBy,
  )
}

fun List<AppointmentInstance>.toModel() = map { it.toModel() }
