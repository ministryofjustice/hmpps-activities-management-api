package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal fun appointmentEntity(
  internalLocationId: Long = 123,
  inCell: Boolean = false,
  createdBy: String = "CREATE.USER",
  updatedBy: String? = "UPDATE.USER",
  prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456),
  numberOfOccurrences: Int = 1,
) = Appointment(
  appointmentId = 1,
  category = appointmentCategoryEntity(),
  prisonCode = "TPR",
  internalLocationId = if (inCell) null else internalLocationId,
  inCell = inCell,
  startDate = LocalDate.now(),
  startTime = LocalTime.of(9, 0),
  endTime = LocalTime.of(10, 30),
  comment = "Appointment level comment",
  created = LocalDateTime.now().minusDays(1),
  createdBy = createdBy,
  updated = LocalDateTime.now(),
  updatedBy = updatedBy,
  deleted = false,
).apply {
  for (i in 1..numberOfOccurrences) {
    val startDate = this.startDate.plusDays(i - 1L)
    this.addOccurrence(appointmentOccurrenceEntity(this, startDate, prisonerNumberToBookingIdMap))
  }
}

internal fun appointmentOccurrenceEntity(appointment: Appointment, startDate: LocalDate = LocalDate.now(), prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456)) =
  AppointmentOccurrence(
    appointmentOccurrenceId = 1,
    appointment = appointment,
    internalLocationId = appointment.internalLocationId,
    inCell = appointment.inCell,
    startDate = appointment.startDate,
    startTime = appointment.startTime,
    endTime = appointment.endTime,
    comment = "Appointment occurrence level comment",
    cancelled = false,
    updated = LocalDateTime.now(),
    updatedBy = "UPDATE.USER",
  ).apply {
    prisonerNumberToBookingIdMap.map { this.addAllocation(appointmentOccurrenceAllocationEntity(this, it.key, it.value)) }
  }.apply {
    prisonerNumberToBookingIdMap.map { this.addInstance(appointmentInstanceEntity(this, startDate, it.key, it.value)) }
  }

internal fun appointmentOccurrenceAllocationEntity(appointmentOccurrence: AppointmentOccurrence, prisonerNumber: String = "A1234BC", bookingId: Long = 456) =
  AppointmentOccurrenceAllocation(
    appointmentOccurrenceAllocationId = 1,
    appointmentOccurrence = appointmentOccurrence,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
  )

internal fun appointmentInstanceEntity(
  appointmentOccurrence: AppointmentOccurrence,
  appointmentDate: LocalDate = LocalDate.now(),
  prisonerNumber: String = "A1234BC",
  bookingId: Long = 456
) =
  AppointmentInstance(
    appointmentInstanceId = 1,
    appointmentOccurrence = appointmentOccurrence,
    category = appointmentCategoryEntity(),
    prisonCode = appointmentOccurrence.appointment.prisonCode,
    internalLocationId = appointmentOccurrence.appointment.internalLocationId,
    inCell = appointmentOccurrence.appointment.inCell,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    appointmentDate = appointmentDate,
    startTime = appointmentOccurrence.appointment.startTime,
    endTime = appointmentOccurrence.appointment.endTime,
    comment = "Appointment instance level comment",
    attended = true,
    cancelled = false,
  )
