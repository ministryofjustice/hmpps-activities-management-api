package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.*

@Entity
@Table(name = "appointment_occurrence_allocation")
data class AppointmentOccurrenceAllocation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentOccurrenceAllocationId: Long = -1,

  @ManyToOne
  @JoinColumn(name = "appointment_occurrence_id", nullable = false)
  val appointmentOccurrence: AppointmentOccurrence,

  val prisonerNumber: String,

  val bookingId: Int
) {
  fun toModel() = uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceAllocation(
    id = appointmentOccurrenceAllocationId,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId
  )
}

fun List<AppointmentOccurrenceAllocation>.toModel() = map { it.toModel() }
