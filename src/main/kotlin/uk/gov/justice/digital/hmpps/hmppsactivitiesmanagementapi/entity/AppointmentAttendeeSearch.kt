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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendee as AppointmentOccurrenceAllocationModel

@Entity
@Table(name = "appointment_attendee")
@EntityListeners(AppointmentAttendeeEntityListener::class)
data class AppointmentAttendeeSearch(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentAttendeeId: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "appointment_id", nullable = false)
  val appointmentSearch: AppointmentSearch,

  val prisonerNumber: String,

  val bookingId: Long,
) {
  fun toModel() = AppointmentOccurrenceAllocationModel(
    id = appointmentAttendeeId,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
  )
}

fun List<AppointmentAttendeeSearch>.toModel() = map { it.toModel() }
