package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
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
