package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceAllocation as AppointmentOccurrenceAllocationModel

@Entity
@Table(name = "appointment_attendee")
@EntityListeners(AppointmentAttendeeEntityListener::class)
data class AppointmentAttendee(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentAttendeeId: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "appointment_id", nullable = false)
  val appointment: Appointment,

  val prisonerNumber: String,

  val bookingId: Long,

  var addedTime: LocalDateTime? = null,

  var addedBy: String? = null,

  var attended: Boolean? = null,

  var attendanceRecordedTime: LocalDateTime? = null,

  var attendanceRecordedBy: String? = null,

  var removedTime: LocalDateTime? = null,

  var removedBy: String? = null,
) {
  fun toModel() = AppointmentOccurrenceAllocationModel(
    id = appointmentAttendeeId,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
  )

  fun isIndividualAppointment() = appointment.appointmentSeries.appointmentType == AppointmentType.INDIVIDUAL

  fun isGroupAppointment() = appointment.appointmentSeries.appointmentType == AppointmentType.GROUP

  fun removeAppointment(appointment: Appointment) = this.appointment.appointmentSeries.removeAppointment(appointment)

  fun removeFromAppointment() = appointment.removeAttendee(this)
}

fun List<AppointmentAttendee>.toModel() = map { it.toModel() }
