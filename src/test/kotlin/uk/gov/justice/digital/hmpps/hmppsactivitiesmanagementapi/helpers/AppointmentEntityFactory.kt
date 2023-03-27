package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal fun appointmentEntity(
  internalLocationId: Long = 123,
  inCell: Boolean = false,
  startDate: LocalDate = LocalDate.now(),
  createdBy: String = "CREATE.USER",
  updatedBy: String? = "UPDATE.USER",
  prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456),
  repeatPeriod: AppointmentRepeatPeriod? = null,
  numberOfOccurrences: Int = 1,
) = Appointment(
  appointmentId = 1,
  categoryCode = "TEST",
  prisonCode = "TPR",
  internalLocationId = if (inCell) null else internalLocationId,
  inCell = inCell,
  startDate = startDate,
  startTime = LocalTime.of(9, 0),
  endTime = LocalTime.of(10, 30),
  comment = "Appointment level comment",
  created = LocalDateTime.now().minusDays(1),
  createdBy = createdBy,
  updated = LocalDateTime.now(),
  updatedBy = updatedBy,
  deleted = false,
).apply {
  repeatPeriod?.let {
    this.schedule = AppointmentSchedule(
      appointment = this,
      repeatPeriod = it,
      repeatCount = numberOfOccurrences,
    )
  }

  this.scheduleIterator().withIndex().forEach {
    this.addOccurrence(appointmentOccurrenceEntity(this, it.index + 1, it.value, prisonerNumberToBookingIdMap))
  }
}

private fun appointmentOccurrenceEntity(appointment: Appointment, sequenceNumber: Int, startDate: LocalDate = LocalDate.now(), prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456)) =
  AppointmentOccurrence(
    appointmentOccurrenceId = 1,
    appointment = appointment,
    sequenceNumber = sequenceNumber,
    internalLocationId = appointment.internalLocationId,
    inCell = appointment.inCell,
    startDate = startDate,
    startTime = appointment.startTime,
    endTime = appointment.endTime,
    comment = "Appointment occurrence level comment",
    cancelled = false,
    updated = LocalDateTime.now(),
    updatedBy = "UPDATE.USER",
  ).apply {
    prisonerNumberToBookingIdMap.map { this.addAllocation(appointmentOccurrenceAllocationEntity(this, it.key, it.value)) }
  }

private fun appointmentOccurrenceAllocationEntity(appointmentOccurrence: AppointmentOccurrence, prisonerNumber: String = "A1234BC", bookingId: Long = 456) =
  AppointmentOccurrenceAllocation(
    appointmentOccurrenceAllocationId = 1,
    appointmentOccurrence = appointmentOccurrence,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
  )

internal fun appointmentInstanceEntity(
  internalLocationId: Long = 123,
  inCell: Boolean = false,
  appointmentDate: LocalDate = LocalDate.now(),
  prisonerNumber: String = "A1234BC",
  bookingId: Long = 456,
  createdBy: String = "CREATE.USER",
  updatedBy: String? = "UPDATE.USER",
) =
  AppointmentInstance(
    appointmentInstanceId = 3,
    appointmentId = 1,
    appointmentOccurrenceId = 2,
    appointmentOccurrenceAllocationId = 3,
    categoryCode = "TEST",
    prisonCode = "TPR",
    internalLocationId = if (inCell) null else internalLocationId,
    inCell = inCell,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    appointmentDate = appointmentDate,
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 30),
    comment = "Appointment instance level comment",
    created = LocalDateTime.now().minusDays(1),
    createdBy = createdBy,
    updated = LocalDateTime.now(),
    updatedBy = updatedBy,
  )
