package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentAttendeeSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AppointmentCancellationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PERMANENT_REMOVAL_BY_USER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.TEMPORARY_REMOVAL_BY_USER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal fun appointmentSeriesEntity(
  appointmentSeriesId: Long = 1,
  appointmentSet: AppointmentSet? = null,
  appointmentType: AppointmentType? = null,
  prisonCode: String = "TPR",
  categoryCode: String = "TEST",
  appointmentTier: EventTier? = eventTier(),
  appointmentOrganiser: EventOrganiser? = eventOrganiser(),
  customName: String? = "Appointment description",
  internalLocationId: Long = 123,
  inCell: Boolean = false,
  startDate: LocalDate = LocalDate.now().plusDays(1),
  startTime: LocalTime = LocalTime.of(9, 0),
  endTime: LocalTime = LocalTime.of(10, 30),
  extraInformation: String? = "Appointment series level comment",
  createdTime: LocalDateTime = LocalDateTime.now().minusDays(1),
  createdBy: String = "CREATE.USER",
  updatedBy: String? = "UPDATE.USER",
  prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456),
  frequency: AppointmentFrequency? = null,
  numberOfAppointments: Int = 1,
  isMigrated: Boolean = false,
  cancelledBy: String? = null,
  cancelledTime: LocalDateTime? = null,
  cancellationReason: AppointmentCancellationReason? = null,
  cancellationStartDate: LocalDate? = null,
  cancellationStartTime: LocalTime? = null,
) = AppointmentSeries(
  appointmentSeriesId = appointmentSeriesId,
  appointmentSet = appointmentSet,
  appointmentType = appointmentType ?: if (prisonerNumberToBookingIdMap.size > 1) AppointmentType.GROUP else AppointmentType.INDIVIDUAL,
  prisonCode = prisonCode,
  categoryCode = categoryCode,
  appointmentTier = appointmentTier,
  customName = customName,
  internalLocationId = internalLocationId,
  inCell = inCell,
  startDate = startDate,
  startTime = startTime,
  endTime = endTime,
  extraInformation = extraInformation,
  createdTime = createdTime,
  createdBy = createdBy,
  updatedTime = if (updatedBy == null) null else LocalDateTime.now(),
  updatedBy = updatedBy,
  isMigrated = isMigrated,
  cancelledBy = cancelledBy,
  cancelledTime = cancelledTime,
  cancellationStartDate = cancellationStartDate,
  cancellationStartTime = cancellationStartTime,
).apply {
  this.appointmentOrganiser = appointmentOrganiser
  appointmentSet?.addAppointmentSeries(this)

  frequency?.let {
    this.schedule = AppointmentSeriesSchedule(
      appointmentSeries = this,
      frequency = it,
      numberOfAppointments = numberOfAppointments,
    )
  }

  this.scheduleIterator().withIndex().forEach {
    this.addAppointment(appointmentEntity(this, appointmentSeriesId * (it.index + 1L), it.index + 1, it.value, this.startTime, updatedTime, updatedBy, prisonerNumberToBookingIdMap, cancellationReason, cancelledBy, cancelledTime))
  }
}

fun appointmentEntity(
  appointmentSeries: AppointmentSeries,
  appointmentId: Long = 1,
  sequenceNumber: Int,
  startDate: LocalDate = LocalDate.now().plusDays(1),
  startTime: LocalTime = appointmentSeries.startTime,
  updatedTime: LocalDateTime? = LocalDateTime.now(),
  updatedBy: String? = "UPDATE.USER",
  prisonerNumberToBookingIdMap: Map<String, Long> = mapOf("A1234BC" to 456),
  cancellationReason: AppointmentCancellationReason? = null,
  cancelledBy: String? = null,
  cancelledTime: LocalDateTime? = null,
) = Appointment(
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
  this.cancellationReason = cancellationReason
  this.cancelledBy = cancelledBy
  this.cancelledTime = cancelledTime
  appointmentOrganiser = appointmentSeries.appointmentOrganiser
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
  isCancelled: Boolean = false,
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
    isCancelled = isCancelled,
    updatedTime = LocalDateTime.now(),
    updatedBy = updatedBy,
    seriesCancellationStartDate = null,
    seriesCancellationStartTime = null,
    seriesFrequency = null,
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
  categoryCode: String = "TEST",
  appointmentTier: EventTier? = eventTier(),
  appointmentOrganiser: EventOrganiser? = eventOrganiser(),
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
    categoryCode = categoryCode,
    customName = customName,
    appointmentTier = appointmentTier,
    internalLocationId = if (inCell) null else 123,
    inCell = inCell,
    startDate = startDate,
    createdTime = LocalDateTime.now().minusDays(1),
    createdBy = "CREATE.USER",
  ).apply {
    this.appointmentOrganiser = appointmentOrganiser

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

internal fun appointmentCreatedInErrorReason() =
  AppointmentCancellationReason(
    1,
    "Created in error",
    true,
  )

internal fun appointmentCancelledReason() =
  AppointmentCancellationReason(
    2,
    "Cancelled",
    false,
  )

internal fun deleteMigratedAppointmentReason() =
  AppointmentCancellationReason(
    3,
    "Delete migrated appointment",
    true,
  )

internal fun permanentRemovalByUserAppointmentAttendeeRemovalReason() =
  AppointmentAttendeeRemovalReason(
    PERMANENT_REMOVAL_BY_USER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
    "Permanent removal by user",
    true,
  )

internal fun tempRemovalByUserAppointmentAttendeeRemovalReason() =
  AppointmentAttendeeRemovalReason(
    TEMPORARY_REMOVAL_BY_USER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
    "Temporary removal by user",
    false,
  )

internal fun cancelOnTransferAppointmentAttendeeRemovalReason() =
  AppointmentAttendeeRemovalReason(
    CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
    "Cancel on transfer - NOMIS OCUCANTR form",
    true,
  )

internal fun prisonerReleasedAppointmentAttendeeRemovalReason() =
  AppointmentAttendeeRemovalReason(
    PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
    "Prisoner status: Released",
    true,
  )

internal fun prisonerPermanentTransferAppointmentAttendeeRemovalReason() =
  AppointmentAttendeeRemovalReason(
    PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
    "Prisoner status: Permanent transfer",
    true,
  )

internal fun appointmentAttendanceSummaryEntity(inCell: Boolean = false, customName: String = "Friday Prayers", categoryCode: String = "CHAP") =
  AppointmentAttendanceSummary(
    1,
    RISLEY_PRISON_CODE,
    categoryCode,
    customName,
    123,
    inCell,
    false,
    true,
    LocalDate.now().plusDays(1),
    LocalTime.of(12, 0),
    LocalTime.of(13, 0),
    false,
    6,
    3,
    2,
    1,
    null,
  )

private fun appointmentAttendeeSearchEntity(appointmentSearch: AppointmentSearch, appointmentAttendeeId: Long = 1, prisonerNumber: String = "A1234BC", bookingId: Long = 456) =
  AppointmentAttendeeSearch(
    appointmentAttendeeId = appointmentAttendeeId,
    appointmentSearch = appointmentSearch,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
  )
