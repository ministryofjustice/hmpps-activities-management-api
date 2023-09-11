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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendeeSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toSummary
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendee as AppointmentAttendeeModel

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
  fun usernames() = listOfNotNull(addedBy, attendanceRecordedBy, removedBy).distinct()

  fun toModel() = AppointmentAttendeeModel(
    id = appointmentAttendeeId,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    addedTime = addedTime,
    addedBy = addedBy,
    attended = attended,
    attendanceRecordedTime = attendanceRecordedTime,
    attendanceRecordedBy = attendanceRecordedBy,
    removedTime = removedTime,
    removedBy = removedBy,
  )

  fun toSummary(prisonerMap: Map<String, Prisoner>) = AppointmentAttendeeSummary(
    id = appointmentAttendeeId,
    prisoner = prisonerMap[prisonerNumber].toSummary(prisonerNumber, bookingId),
    attended = attended,
  )
}

fun List<AppointmentAttendee>.toModel() = map { it.toModel() }

fun List<AppointmentAttendee>.toSummary(prisonerMap: Map<String, Prisoner>) = map { it.toSummary(prisonerMap) }
