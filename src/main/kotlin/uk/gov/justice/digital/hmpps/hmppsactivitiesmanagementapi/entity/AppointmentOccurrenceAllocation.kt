package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceAllocation as AppointmentOccurrenceAllocationModel

@Entity
@Table(name = "appointment_occurrence_allocation")
@EntityListeners(AppointmentOccurrenceAllocationEntityListener::class)
data class AppointmentOccurrenceAllocation(
  @Id
  // @GeneratedValue(generator = "appointment_occurrence_alloca_appointment_occurrence_alloca_seq")
  @SequenceGenerator(name = "appointment_occurrence_allocation_seq", sequenceName = "appointment_occurrence_alloca_appointment_occurrence_alloca_seq", allocationSize = 1)
  val appointmentOccurrenceAllocationId: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "appointment_occurrence_id", nullable = false)
  val appointmentOccurrence: AppointmentOccurrence,

  val prisonerNumber: String,

  val bookingId: Long,

) {
  fun toModel() = AppointmentOccurrenceAllocationModel(
    id = appointmentOccurrenceAllocationId,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
  )

  fun isIndividualAppointment() = appointmentOccurrence.appointment.appointmentType == AppointmentType.INDIVIDUAL

  fun isGroupAppointment() = appointmentOccurrence.appointment.appointmentType == AppointmentType.GROUP

  fun removeOccurrence(occurrence: AppointmentOccurrence) = appointmentOccurrence.appointment.removeOccurrence(occurrence)

  fun removeFromAppointmentOccurrence() = appointmentOccurrence.removeAllocation(this)
}

fun List<AppointmentOccurrenceAllocation>.toModel() = map { it.toModel() }
