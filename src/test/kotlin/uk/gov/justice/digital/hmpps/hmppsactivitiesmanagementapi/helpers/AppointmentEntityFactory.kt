package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocationSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal fun appointmentEntity(
  appointmentId: Long = 1,
  appointmentType: AppointmentType? = null,
  internalLocationId: Long = 123,
  inCell: Boolean = false,
  startDate: LocalDate = LocalDate.now().plusDays(1),
  startTime: LocalTime = LocalTime.of(9, 0),
  endTime: LocalTime = LocalTime.of(10, 30),
  createdBy: String = "CREATE.USER",
  updatedBy: String? = "UPDATE.USER",
  prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456),
  repeatPeriod: AppointmentRepeatPeriod? = null,
  numberOfOccurrences: Int = 1,
) = Appointment(
  appointmentId = appointmentId,
  appointmentType = appointmentType ?: if (prisonerNumberToBookingIdMap.size > 1) AppointmentType.GROUP else AppointmentType.INDIVIDUAL,
  prisonCode = "TPR",
  categoryCode = "TEST",
  internalLocationId = if (inCell) null else internalLocationId,
  inCell = inCell,
  startDate = startDate,
  startTime = startTime,
  endTime = endTime,
  comment = "Appointment level comment",
  appointmentDescription = "Appointment description",
  created = LocalDateTime.now().minusDays(1),
  createdBy = createdBy,
  updated = if (updatedBy == null) null else LocalDateTime.now(),
  updatedBy = updatedBy,
).apply {
  repeatPeriod?.let {
    this.schedule = AppointmentSchedule(
      appointment = this,
      repeatPeriod = it,
      repeatCount = numberOfOccurrences,
    )
  }

  this.scheduleIterator().withIndex().forEach {
    this.addOccurrence(appointmentOccurrenceEntity(this, it.index + 1L, it.index + 1, it.value, updatedBy, prisonerNumberToBookingIdMap))
  }
}

private fun appointmentOccurrenceEntity(appointment: Appointment, appointmentOccurrenceId: Long = 1, sequenceNumber: Int, startDate: LocalDate = LocalDate.now(), updatedBy: String? = "UPDATE.USER", prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456)) =
  AppointmentOccurrence(
    appointmentOccurrenceId = appointmentOccurrenceId,
    appointment = appointment,
    sequenceNumber = sequenceNumber,
    internalLocationId = appointment.internalLocationId,
    inCell = appointment.inCell,
    startDate = startDate,
    startTime = appointment.startTime,
    endTime = appointment.endTime,
    comment = "Appointment occurrence level comment",
    updated = if (updatedBy == null) null else LocalDateTime.now(),
    updatedBy = updatedBy,
  ).apply {
    prisonerNumberToBookingIdMap.map {
      val appointmentOccurrenceAllocationId = prisonerNumberToBookingIdMap.size * (appointmentOccurrenceId - 1) + this.allocations().size + 1
      this.addAllocation(appointmentOccurrenceAllocationEntity(this, appointmentOccurrenceAllocationId, it.key, it.value))
    }
  }

private fun appointmentOccurrenceAllocationEntity(appointmentOccurrence: AppointmentOccurrence, appointmentOccurrenceAllocationId: Long = 1, prisonerNumber: String = "A1234BC", bookingId: Long = 456) =
  AppointmentOccurrenceAllocation(
    appointmentOccurrenceAllocationId = appointmentOccurrenceAllocationId,
    appointmentOccurrence = appointmentOccurrence,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
  )

internal fun appointmentInstanceEntity(
  prisonerNumber: String = "A1234BC",
  bookingId: Long = 456,
  internalLocationId: Long = 123,
  inCell: Boolean = false,
  appointmentDate: LocalDate = LocalDate.now(),
  createdBy: String = "CREATE.USER",
  updatedBy: String? = "UPDATE.USER",
  appointmentDescription: String? = null,
) =
  AppointmentInstance(
    appointmentInstanceId = 3,
    appointmentId = 1,
    appointmentOccurrenceId = 2,
    appointmentOccurrenceAllocationId = 3,
    appointmentType = AppointmentType.INDIVIDUAL,
    prisonCode = "TPR",
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    categoryCode = "TEST",
    appointmentDescription = appointmentDescription,
    internalLocationId = if (inCell) null else internalLocationId,
    inCell = inCell,
    appointmentDate = appointmentDate,
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 30),
    comment = "Appointment instance level comment",
    created = LocalDateTime.now().minusDays(1),
    createdBy = createdBy,
    isCancelled = false,
    updated = LocalDateTime.now(),
    updatedBy = updatedBy,
  )

internal fun appointmentOccurrenceSearchEntity(
  prisonerNumber: String = "A1234BC",
  bookingId: Long = 456,
  internalLocationId: Long = 123,
  inCell: Boolean = false,
  startDate: LocalDate = LocalDate.now(),
  createdBy: String = "CREATE.USER",
) =
  AppointmentOccurrenceSearch(
    appointmentId = 1,
    appointmentOccurrenceId = 2,
    appointmentType = AppointmentType.INDIVIDUAL,
    prisonCode = "TPR",
    categoryCode = "TEST",
    appointmentDescription = null,
    internalLocationId = if (inCell) null else internalLocationId,
    inCell = inCell,
    startDate = startDate,
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 30),
    isRepeat = false,
    sequenceNumber = 1,
    maxSequenceNumber = 1,
    comment = "Appointment occurrence level comment",
    createdBy = createdBy,
    isEdited = false,
    isCancelled = false,
  ).apply {
    allocations = listOf(
      appointmentOccurrenceAllocationSearchEntity(
        this,
        prisonerNumber = prisonerNumber,
        bookingId = bookingId,
      ),
    )
  }

private fun appointmentOccurrenceAllocationSearchEntity(appointmentOccurrenceSearch: AppointmentOccurrenceSearch, appointmentOccurrenceAllocationId: Long = 1, prisonerNumber: String = "A1234BC", bookingId: Long = 456) =
  AppointmentOccurrenceAllocationSearch(
    appointmentOccurrenceAllocationId = appointmentOccurrenceAllocationId,
    appointmentOccurrenceSearch = appointmentOccurrenceSearch,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
  )
