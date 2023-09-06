package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.*
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceAllocation as AppointmentOccurrenceAllocationModel

@Entity
@Table(name = "appointment_attendee")
@EntityListeners(AppointmentOccurrenceAllocationEntityListener::class)
data class AppointmentOccurrenceAllocationSearch(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "appointment_attendee_id")
  val appointmentOccurrenceAllocationId: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "appointment_id", nullable = false)
  val appointmentOccurrenceSearch: AppointmentOccurrenceSearch,

  val prisonerNumber: String,

  val bookingId: Long,
) {
  fun toModel() = AppointmentOccurrenceAllocationModel(
    id = appointmentOccurrenceAllocationId,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
  )
}

fun List<AppointmentOccurrenceAllocationSearch>.toModel() = map { it.toModel() }
