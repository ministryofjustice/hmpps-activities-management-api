package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal fun appointmentEntity(inCell: Boolean = false) =
  Appointment(
    appointmentId = 1,
    category = appointmentCategoryEntity(),
    prisonCode = "TPR",
    internalLocationId = if (inCell) null else 123,
    inCell = inCell,
    startDate = LocalDate.now(),
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 30),
    comment = "Appointment level comment",
    created = LocalDateTime.now().minusDays(1),
    createdBy = "CREATE.USER",
    updated = LocalDateTime.now(),
    updatedBy = "UPDATE.USER",
    deleted = false,
  ).apply {
    this.addOccurrence(appointmentOccurrenceEntity(this))
  }

internal fun appointmentOccurrenceEntity(appointment: Appointment) =
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
    this.addAllocation(appointmentOccurrenceAllocationEntity(this))
  }
    .apply {
      this.addInstance(appointmentInstanceEntity(this))
    }

internal fun appointmentOccurrenceAllocationEntity(appointmentOccurrence: AppointmentOccurrence) =
  AppointmentOccurrenceAllocation(
    appointmentOccurrenceAllocationId = 1,
    appointmentOccurrence = appointmentOccurrence,
    prisonerNumber = "A1234BC",
    bookingId = 456,
  )

internal fun appointmentInstanceEntity(appointmentOccurrence: AppointmentOccurrence, appointmentDate: LocalDate = LocalDate.now()) =
  AppointmentInstance(
    appointmentInstanceId = 1,
    appointmentOccurrence = appointmentOccurrence,
    category = appointmentCategoryEntity(),
    prisonCode = "TPR",
    internalLocationId = 123,
    inCell = false,
    prisonerNumber = "A1234BC",
    bookingId = 456,
    appointmentDate = appointmentDate,
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 30),
    comment = "Appointment instance level comment",
    attended = true,
    cancelled = false,
  )
