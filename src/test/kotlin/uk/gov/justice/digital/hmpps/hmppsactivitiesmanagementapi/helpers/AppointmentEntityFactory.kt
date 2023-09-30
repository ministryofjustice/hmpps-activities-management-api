package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendeeSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancellationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentHost
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.NOT_SPECIFIED_APPOINTMENT_TIER_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.NO_TIER_APPOINTMENT_TIER_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISON_STAFF_APPOINTMENT_HOST_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.TEMPORARY_REMOVAL_BY_USER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.TIER_1_APPOINTMENT_TIER_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.TIER_2_APPOINTMENT_TIER_ID
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal fun appointmentSeriesEntity(
  appointmentSeriesId: Long = 1,
  appointmentSet: AppointmentSet? = null,
  appointmentType: AppointmentType? = null,
  prisonCode: String = "TPR",
  categoryCode: String = "TEST",
  customName: String? = "Appointment description",
  internalLocationId: Long = 123,
  inCell: Boolean = false,
  startDate: LocalDate = LocalDate.now().plusDays(1),
  startTime: LocalTime = LocalTime.of(9, 0),
  endTime: LocalTime = LocalTime.of(10, 30),
  createdTime: LocalDateTime = LocalDateTime.now().minusDays(1),
  createdBy: String = "CREATE.USER",
  updatedBy: String? = "UPDATE.USER",
  prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456),
  frequency: AppointmentFrequency? = null,
  numberOfAppointments: Int = 1,
  isMigrated: Boolean = false,
) = AppointmentSeries(
  appointmentSeriesId = appointmentSeriesId,
  appointmentSet = appointmentSet,
  appointmentType = appointmentType ?: if (prisonerNumberToBookingIdMap.size > 1) AppointmentType.GROUP else AppointmentType.INDIVIDUAL,
  prisonCode = prisonCode,
  categoryCode = categoryCode,
  customName = customName,
  appointmentTier = appointmentTierNotSpecified(),
  internalLocationId = internalLocationId,
  inCell = inCell,
  startDate = startDate,
  startTime = startTime,
  endTime = endTime,
  extraInformation = "Appointment series level comment",
  createdTime = createdTime,
  createdBy = createdBy,
  updatedTime = if (updatedBy == null) null else LocalDateTime.now(),
  updatedBy = updatedBy,
  isMigrated = isMigrated,
).apply {
  appointmentSet?.addAppointmentSeries(this)

  frequency?.let {
    this.schedule = AppointmentSeriesSchedule(
      appointmentSeries = this,
      frequency = it,
      numberOfAppointments = numberOfAppointments,
    )
  }

  this.scheduleIterator().withIndex().forEach {
    this.addAppointment(appointmentEntity(this, appointmentSeriesId * (it.index + 1L), it.index + 1, it.value, this.startTime, updatedTime, updatedBy, prisonerNumberToBookingIdMap))
  }
}

fun appointmentEntity(appointmentSeries: AppointmentSeries, appointmentId: Long = 1, sequenceNumber: Int, startDate: LocalDate = LocalDate.now().plusDays(1), startTime: LocalTime = appointmentSeries.startTime, updatedTime: LocalDateTime? = LocalDateTime.now(), updatedBy: String? = "UPDATE.USER", prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456)) =
  Appointment(
    appointmentId = appointmentId,
    appointmentSeries = appointmentSeries,
    sequenceNumber = sequenceNumber,
    prisonCode = appointmentSeries.prisonCode,
    categoryCode = appointmentSeries.categoryCode,
    customName = appointmentSeries.customName,
    appointmentTier = appointmentSeries.appointmentTier,
    internalLocationId = appointmentSeries.internalLocationId,
    inCell = appointmentSeries.inCell,
    startDate = startDate,
    startTime = startTime,
    endTime = appointmentSeries.endTime,
    extraInformation = "Appointment level comment",
    createdTime = appointmentSeries.createdTime,
    createdBy = appointmentSeries.createdBy,
    updatedTime = updatedTime,
    updatedBy = updatedBy,
  ).apply {
    prisonerNumberToBookingIdMap.map {
      val appointmentAttendeeId = prisonerNumberToBookingIdMap.size * (appointmentId - 1) + this.attendees().size + 1
      this.addAttendee(appointmentAttendeeEntity(this, appointmentAttendeeId, it.key, it.value))
    }
  }

private fun appointmentAttendeeEntity(appointment: Appointment, appointmentAttendeeId: Long = 1, prisonerNumber: String = "A1234BC", bookingId: Long = 456) =
  AppointmentAttendee(
    appointmentAttendeeId = appointmentAttendeeId,
    appointment = appointment,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
  )

internal fun appointmentInstanceEntity(
  appointmentInstanceId: Long = 3,
  prisonerNumber: String = "A1234BC",
  bookingId: Long = 456,
  internalLocationId: Long = 123,
  inCell: Boolean = false,
  appointmentDate: LocalDate = LocalDate.now().plusDays(1),
  createdBy: String = "CREATE.USER",
  updatedBy: String? = "UPDATE.USER",
  customName: String? = null,
  categoryCode: String = "TEST",
) =
  AppointmentInstance(
    appointmentInstanceId = appointmentInstanceId,
    appointmentSeriesId = 1,
    appointmentId = 2,
    appointmentAttendeeId = appointmentInstanceId,
    appointmentType = AppointmentType.INDIVIDUAL,
    prisonCode = "TPR",
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    categoryCode = categoryCode,
    customName = customName,
    internalLocationId = if (inCell) null else internalLocationId,
    customLocation = null,
    inCell = inCell,
    onWing = false,
    offWing = true,
    appointmentDate = appointmentDate,
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 30),
    unlockNotes = null,
    extraInformation = "Appointment level comment",
    createdTime = LocalDateTime.now().minusDays(1),
    createdBy = createdBy,
    isCancelled = false,
    updatedTime = LocalDateTime.now(),
    updatedBy = updatedBy,
  )

internal fun appointmentSearchEntity(
  appointmentSeriesId: Long = 1,
  appointmentId: Long = 2,
  appointmentType: AppointmentType = AppointmentType.INDIVIDUAL,
  prisonCode: String = "TPR",
  appointmentAttendeeId: Long = 1,
  prisonerNumber: String = "A1234BC",
  bookingId: Long = 456,
  internalLocationId: Long = 123,
  inCell: Boolean = false,
  startDate: LocalDate = LocalDate.now(),
  startTime: LocalTime = LocalTime.of(9, 0),
  endTime: LocalTime = LocalTime.of(10, 30),
  createdBy: String = "CREATE.USER",
) =
  AppointmentSearch(
    appointmentSeriesId = appointmentSeriesId,
    appointmentId = appointmentId,
    appointmentType = appointmentType,
    prisonCode = prisonCode,
    categoryCode = "TEST",
    customName = null,
    internalLocationId = if (inCell) null else internalLocationId,
    customLocation = null,
    inCell = inCell,
    onWing = false,
    offWing = true,
    startDate = startDate.plusDays(1),
    startTime = startTime,
    endTime = endTime,
    isRepeat = false,
    sequenceNumber = 1,
    maxSequenceNumber = 1,
    unlockNotes = null,
    extraInformation = "Appointment level comment",
    createdBy = createdBy,
    isEdited = false,
    isCancelled = false,
  ).apply {
    attendees = listOf(
      appointmentAttendeeSearchEntity(
        appointmentSearch = this,
        appointmentAttendeeId = appointmentAttendeeId,
        prisonerNumber = prisonerNumber,
        bookingId = bookingId,
      ),
    )
  }

internal fun appointmentSetEntity(
  appointmentSetId: Long = 1,
  inCell: Boolean = false,
  customName: String? = null,
  startDate: LocalDate = LocalDate.now().plusDays(1),
  startTime: LocalTime = LocalTime.of(9, 0),
  endTime: LocalTime = LocalTime.of(10, 30),
  prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456, "B2345CD" to 457, "C3456DE" to 458),
) =
  AppointmentSet(
    appointmentSetId = appointmentSetId,
    prisonCode = "TPR",
    categoryCode = "TEST",
    customName = customName,
    appointmentTier = appointmentTierNotSpecified(),
    internalLocationId = if (inCell) null else 123,
    inCell = inCell,
    startDate = startDate,
    createdTime = LocalDateTime.now().minusDays(1),
    createdBy = "CREATE.USER",
  ).apply {
    var count = 0L
    prisonerNumberToBookingIdMap.forEach {
      appointmentSeriesEntity(
        count + 1,
        this,
        customName = customName,
        startTime = startTime.plusMinutes(30 * count),
        endTime = endTime.plusMinutes(30 * count),
        prisonerNumberToBookingIdMap = mapOf(it.toPair()),
        updatedBy = null,
        createdTime = createdTime,
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

internal fun appointmentHostPrisonStaff() =
  AppointmentHost(
    PRISON_STAFF_APPOINTMENT_HOST_ID,
    "Prison staff",
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

internal fun appointmentAttendeeRemovedReason() =
  AppointmentAttendeeRemovalReason(
    TEMPORARY_REMOVAL_BY_USER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
    "Temporary removal by user",
    false,
  )

internal fun appointmentAttendeeDeletedReason() =
  AppointmentAttendeeRemovalReason(
    PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
    "Prisoner status: Released",
    true,
  )

private fun appointmentAttendeeSearchEntity(appointmentSearch: AppointmentSearch, appointmentAttendeeId: Long = 1, prisonerNumber: String = "A1234BC", bookingId: Long = 456) =
  AppointmentAttendeeSearch(
    appointmentAttendeeId = appointmentAttendeeId,
    appointmentSearch = appointmentSearch,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
  )
