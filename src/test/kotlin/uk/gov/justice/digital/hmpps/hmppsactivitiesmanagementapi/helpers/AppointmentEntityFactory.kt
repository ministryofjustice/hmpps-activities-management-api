package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancellationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocationSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.BulkAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.NO_TIER_APPOINTMENT_TIER_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.NOT_SPECIFIED_APPOINTMENT_TIER_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.TIER_1_APPOINTMENT_TIER_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.TIER_2_APPOINTMENT_TIER_ID
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal fun appointmentEntity(
  appointmentId: Long = 1,
  bulkAppointment: BulkAppointment? = null,
  appointmentType: AppointmentType? = null,
  appointmentDescription: String? = "Appointment description",
  internalLocationId: Long = 123,
  inCell: Boolean = false,
  startDate: LocalDate = LocalDate.now().plusDays(1),
  startTime: LocalTime = LocalTime.of(9, 0),
  endTime: LocalTime = LocalTime.of(10, 30),
  created: LocalDateTime = LocalDateTime.now().minusDays(1),
  createdBy: String = "CREATE.USER",
  updatedBy: String? = "UPDATE.USER",
  prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456),
  repeatPeriod: AppointmentRepeatPeriod? = null,
  numberOfOccurrences: Int = 1,
) = Appointment(
  appointmentId = appointmentId,
  bulkAppointment = bulkAppointment,
  appointmentType = appointmentType ?: if (prisonerNumberToBookingIdMap.size > 1) AppointmentType.GROUP else AppointmentType.INDIVIDUAL,
  prisonCode = "TPR",
  categoryCode = "TEST",
  appointmentDescription = appointmentDescription,
  appointmentTier = appointmentTierNotSpecified(),
  internalLocationId = internalLocationId,
  inCell = inCell,
  startDate = startDate,
  startTime = startTime,
  endTime = endTime,
  comment = "Appointment level comment",
  created = created,
  createdBy = createdBy,
  updated = if (updatedBy == null) null else LocalDateTime.now(),
  updatedBy = updatedBy,
).apply {
  bulkAppointment?.addAppointment(this)

  repeatPeriod?.let {
    this.schedule = AppointmentSchedule(
      appointment = this,
      repeatPeriod = it,
      repeatCount = numberOfOccurrences,
    )
  }

  this.scheduleIterator().withIndex().forEach {
    this.addOccurrence(appointmentOccurrenceEntity(this, appointmentId * (it.index + 1L), it.index + 1, it.value, this.startTime, updated, updatedBy, prisonerNumberToBookingIdMap))
  }
}

fun appointmentOccurrenceEntity(appointment: Appointment, appointmentOccurrenceId: Long = 1, sequenceNumber: Int, startDate: LocalDate = LocalDate.now().plusDays(1), startTime: LocalTime = appointment.startTime, updated: LocalDateTime? = LocalDateTime.now(), updatedBy: String? = "UPDATE.USER", prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456)) =
  AppointmentOccurrence(
    appointmentOccurrenceId = appointmentOccurrenceId,
    appointment = appointment,
    sequenceNumber = sequenceNumber,
    prisonCode = appointment.prisonCode,
    categoryCode = appointment.categoryCode,
    appointmentDescription = appointment.appointmentDescription,
    appointmentTier = appointment.appointmentTier,
    internalLocationId = appointment.internalLocationId,
    inCell = appointment.inCell,
    startDate = startDate,
    startTime = startTime,
    endTime = appointment.endTime,
    comment = "Appointment occurrence level comment",
    created = appointment.created,
    createdBy = appointment.createdBy,
    updated = updated,
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
  appointmentDate: LocalDate = LocalDate.now().plusDays(1),
  createdBy: String = "CREATE.USER",
  updatedBy: String? = "UPDATE.USER",
  appointmentDescription: String? = null,
  categoryCode: String = "TEST",
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
    categoryCode = categoryCode,
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
    startDate = startDate.plusDays(1),
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

internal fun bulkAppointmentEntity(
  bulkAppointmentId: Long = 1,
  inCell: Boolean = false,
  appointmentDescription: String? = null,
  startDate: LocalDate = LocalDate.now().plusDays(1),
  startTime: LocalTime = LocalTime.of(9, 0),
  endTime: LocalTime = LocalTime.of(10, 30),
  prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456, "B2345CD" to 457, "C3456DE" to 458),
) =
  BulkAppointment(
    bulkAppointmentId = bulkAppointmentId,
    prisonCode = "TPR",
    categoryCode = "TEST",
    appointmentDescription = appointmentDescription,
    appointmentTier = appointmentTierNotSpecified(),
    internalLocationId = if (inCell) null else 123,
    inCell = inCell,
    startDate = startDate,
    created = LocalDateTime.now().minusDays(1),
    createdBy = "CREATE.USER",
  ).apply {
    var count = 0L
    prisonerNumberToBookingIdMap.forEach {
      appointmentEntity(
        count + 1,
        this,
        appointmentDescription = appointmentDescription,
        startTime = startTime.plusMinutes(30 * count),
        endTime = endTime.plusMinutes(30 * count),
        prisonerNumberToBookingIdMap = mapOf(it.toPair()),
        updatedBy = null,
        created = created,
      )
      count++
    }
  }

internal fun appointmentTier1() =
  AppointmentTier(
    TIER_1_APPOINTMENT_TIER_ID,
    "Tier 1",
  )

internal fun appointmentTier2() =
  AppointmentTier(
    TIER_2_APPOINTMENT_TIER_ID,
    "Tier 2",
  )

internal fun appointmentNoTier() =
  AppointmentTier(
    NO_TIER_APPOINTMENT_TIER_ID,
    "No tier, this activity is not considered 'purposeful' for reporting",
  )

internal fun appointmentTierNotSpecified() =
  AppointmentTier(
    NOT_SPECIFIED_APPOINTMENT_TIER_ID,
    "Not specified",
  )

internal fun appointmentCancelledReason() =
  AppointmentCancellationReason(
    2,
    "Cancelled",
    false,
  )

internal fun appointmentDeletedReason() =
  AppointmentCancellationReason(
    1,
    "Created in error",
    true,
  )

private fun appointmentOccurrenceAllocationSearchEntity(appointmentOccurrenceSearch: AppointmentOccurrenceSearch, appointmentOccurrenceAllocationId: Long = 1, prisonerNumber: String = "A1234BC", bookingId: Long = 456) =
  AppointmentOccurrenceAllocationSearch(
    appointmentOccurrenceAllocationId = appointmentOccurrenceAllocationId,
    appointmentOccurrenceSearch = appointmentOccurrenceSearch,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
  )
