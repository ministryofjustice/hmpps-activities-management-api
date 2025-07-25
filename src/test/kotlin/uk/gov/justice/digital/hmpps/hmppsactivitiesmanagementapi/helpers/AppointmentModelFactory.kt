package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendeeSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentAttendeeSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventTier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

fun appointmentCategorySummary() = AppointmentCategorySummary("TEST", "Test Category")

fun appointmentSeriesModel(
  createdTime: LocalDateTime = LocalDateTime.now(),
  updatedTime: LocalDateTime? = null,
  appointmentUpdatedTime: LocalDateTime? = null,
) = AppointmentSeries(
  id = 1,
  appointmentType = AppointmentType.INDIVIDUAL,
  prisonCode = "TPR",
  categoryCode = "TEST",
  tier = eventTier().toModelEventTier(),
  organiser = eventOrganiser().toModelEventOrganiser(),
  customName = "Appointment description",
  internalLocationId = 123,
  dpsLocationId = UUID.fromString("44444444-1111-2222-3333-444444444444"),
  inCell = false,
  startDate = LocalDate.now().plusDays(1),
  startTime = LocalTime.of(9, 0),
  endTime = LocalTime.of(10, 30),
  schedule = null,
  extraInformation = "Appointment series level comment",
  createdTime = createdTime,
  createdBy = "CREATE.USER",
  updatedTime = updatedTime,
  updatedBy = "UPDATE.USER",
  appointments = listOf(appointmentModel(createdTime, appointmentUpdatedTime)),
)

fun appointmentAttendeeModel() = AppointmentAttendee(1, "A1234BC", 456, null, null, null, null, null, null, null, null)

fun appointmentAttendeeSearchResultModel() = AppointmentAttendeeSearchResult(1, "A1234BC", 456)

fun appointmentModel(createdTime: LocalDateTime = LocalDateTime.now(), updatedTime: LocalDateTime? = null) = Appointment(
  1,
  1,
  "TPR",
  "TEST",
  eventTier().toModelEventTier(),
  eventOrganiser().toModelEventOrganiser(),
  "Appointment description",
  123,
  dpsLocationId = UUID.fromString("44444444-1111-2222-3333-444444444444"),
  false,
  LocalDate.now().plusDays(1),
  LocalTime.of(9, 0),
  LocalTime.of(10, 30),
  "Appointment level comment",
  createdTime,
  "CREATE.USER",
  updatedTime,
  "UPDATE.USER",
  null,
  null,
  null,
  false,
  attendees = listOf(appointmentAttendeeModel()),
)

fun appointmentInstanceModel(
  createdTime: LocalDateTime = LocalDateTime.now().minusDays(1),
  updatedTime: LocalDateTime? = LocalDateTime.now(),
) = AppointmentInstance(
  id = 3,
  appointmentSeriesId = 1,
  appointmentId = 2,
  appointmentAttendeeId = 3,
  appointmentType = AppointmentType.INDIVIDUAL,
  prisonCode = "TPR",
  prisonerNumber = "A1234BC",
  bookingId = 456,
  categoryCode = "TEST",
  customName = null,
  internalLocationId = 123,
  dpsLocationId = UUID.fromString("44444444-1111-2222-3333-444444444444"),
  inCell = false,
  appointmentDate = LocalDate.now().plusDays(1),
  startTime = LocalTime.of(9, 0),
  endTime = LocalTime.of(10, 30),
  extraInformation = "Appointment level comment",
  createdTime = createdTime,
  createdBy = "CREATE.USER",
  updatedTime = updatedTime,
  updatedBy = "UPDATE.USER",
)

fun appointmentSeriesCreateRequest(
  appointmentType: AppointmentType = AppointmentType.INDIVIDUAL,
  prisonCode: String? = "TPR",
  prisonerNumbers: List<String> = listOf("A1234BC"),
  categoryCode: String? = "TEST",
  tierCode: String? = eventTier().code,
  organiserCode: String? = eventOrganiser().code,
  customName: String? = "Appointment description",
  internalLocationId: Long? = 123,
  dpsLocationId: UUID? = UUID.fromString("44444444-1111-2222-3333-444444444444"),
  inCell: Boolean = false,
  startDate: LocalDate? = LocalDate.now().plusDays(1),
  startTime: LocalTime? = LocalTime.of(13, 0),
  endTime: LocalTime? = LocalTime.of(14, 30),
  extraInformation: String? = "Appointment level comment",
  schedule: AppointmentSeriesSchedule? = null,
  originalAppointmentId: Long? = 0L,
) = AppointmentSeriesCreateRequest(
  appointmentType = appointmentType,
  prisonCode = prisonCode,
  prisonerNumbers = prisonerNumbers,
  categoryCode = categoryCode,
  tierCode = tierCode,
  organiserCode = organiserCode,
  customName = customName,
  internalLocationId = internalLocationId,
  dpsLocationId = dpsLocationId,
  inCell = inCell,
  startDate = startDate,
  startTime = startTime,
  endTime = endTime,
  schedule = schedule,
  extraInformation = extraInformation,
  originalAppointmentId = originalAppointmentId,
)

fun appointmentSetCreateRequest(
  prisonCode: String? = "TPR",
  categoryCode: String? = "TEST",
  tierCode: String? = eventTier().code,
  organiserCode: String? = eventOrganiser().code,
  customName: String? = "Appointment description",
  internalLocationId: Long? = 123,
  dpsLocationId: UUID? = UUID.fromString("44444444-1111-2222-3333-444444444444"),
  inCell: Boolean = false,
  startDate: LocalDate? = LocalDate.now().plusDays(1),
  startTime: LocalTime? = LocalTime.of(13, 0),
  endTime: LocalTime? = LocalTime.of(14, 30),
  extraInformation: String? = "Test comment",
  prisonerNumbers: List<String?> = listOf("A1234BC", "A1234BD"),
) = AppointmentSetCreateRequest(
  categoryCode = categoryCode,
  tierCode = tierCode,
  organiserCode = organiserCode,
  prisonCode = prisonCode,
  internalLocationId = internalLocationId,
  dpsLocationId = dpsLocationId,
  inCell = inCell,
  startDate = startDate,
  customName = customName,
  appointments = prisonerNumbers.map {
    AppointmentSetAppointment(
      prisonerNumber = it,
      startTime = startTime,
      endTime = endTime,
      extraInformation = extraInformation,
    )
  }.toList(),
)

fun appointmentMigrateRequest(
  prisonCode: String? = "TPR",
  prisonerNumber: String? = "A1234BC",
  bookingId: Long? = 123,
  categoryCode: String? = "TEST",
  dpsLocationId: UUID? = UUID.fromString("44444444-1111-2222-3333-444444444444"),
  startDate: LocalDate? = LocalDate.now().plusDays(1),
  startTime: LocalTime? = LocalTime.of(13, 0),
  endTime: LocalTime? = LocalTime.of(14, 30),
  comment: String? = "Appointment level comment",
  createdTime: LocalDateTime? = LocalDateTime.now(),
  createdBy: String? = "CREATE.USER",
  updatedTime: LocalDateTime? = null,
  updatedBy: String? = null,
  isCancelled: Boolean? = false,
) = AppointmentMigrateRequest(
  prisonCode,
  prisonerNumber,
  bookingId,
  categoryCode,
  dpsLocationId,
  startDate,
  startTime,
  endTime,
  comment,
  isCancelled,
  createdTime,
  createdBy,
  updatedTime,
  updatedBy,
)

fun appointmentSeriesDetails(
  customName: String? = null,
  category: AppointmentCategorySummary = appointmentCategorySummary(),
  createdTime: LocalDateTime = LocalDateTime.now(),
  updatedTime: LocalDateTime? = LocalDateTime.now(),
  updatedBy: String? = "UPDATE.USER",
) = AppointmentSeriesDetails(
  1,
  AppointmentType.INDIVIDUAL,
  "TPR",
  if (!customName.isNullOrEmpty()) "$customName (${category.description})" else category.description,
  category = category,
  eventTier().toModelEventTier(),
  eventOrganiser().toModelEventOrganiser(),
  customName,
  AppointmentLocationSummary(123, UUID.fromString("44444444-1111-2222-3333-444444444444"), "TPR", "Test Appointment Location"),
  false,
  LocalDate.now().plusDays(1),
  LocalTime.of(9, 0),
  LocalTime.of(10, 30),
  null,
  "Appointment series level comment",
  createdTime,
  "CREATE.USER",
  updatedTime,
  updatedBy,
  appointments = listOf(
    AppointmentSummary(
      1,
      1,
      LocalDate.now().plusDays(1),
      LocalTime.of(9, 0),
      LocalTime.of(10, 30),
      isEdited = updatedTime != null,
      isCancelled = false,
      isDeleted = false,
    ),
  ),
)

fun appointmentDetails(
  appointmentId: Long = 1,
  appointmentSeriesId: Long? = 2,
  appointmentSetSummary: AppointmentSetSummary? = null,
  sequenceNumber: Int = 3,
  prisoners: List<PrisonerSummary> = listOf(
    PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "ACTIVE IN", "TPR", "1-2-3", "H"),
  ),
  category: AppointmentCategorySummary = appointmentCategorySummary(),
  customName: String? = null,
  startTime: LocalTime = LocalTime.of(9, 0),
  endTime: LocalTime = LocalTime.of(10, 30),
  extraInformation: String = "Appointment level comment",
  createdTime: LocalDateTime = LocalDateTime.now(),
  createdBy: String = "CREATE.USER",
  updatedTime: LocalDateTime? = LocalDateTime.now(),
  updatedBy: String? = "UPDATE.USER",
  appointmentAttendeeId: Long = 1,
) = AppointmentDetails(
  appointmentId,
  if (appointmentSeriesId != null) AppointmentSeriesSummary(appointmentSeriesId, null, sequenceNumber, sequenceNumber) else null,
  appointmentSetSummary,
  AppointmentType.INDIVIDUAL,
  sequenceNumber,
  "TPR",
  if (!customName.isNullOrEmpty()) "$customName (${category.description})" else category.description,
  prisoners.map { AppointmentAttendeeSummary(appointmentAttendeeId, it, null, null, null) },
  category,
  eventTier().toModelEventTier(),
  eventOrganiser().toModelEventOrganiser(),
  customName,
  AppointmentLocationSummary(123, UUID.fromString("44444444-1111-2222-3333-444444444444"), "TPR", "Test Appointment Location"),
  false,
  LocalDate.now().plusDays(1),
  startTime,
  endTime,
  false,
  extraInformation,
  createdTime,
  createdBy,
  updatedTime != null,
  updatedTime,
  updatedBy,
  false,
  false,
  null,
  null,
)

fun appointmentSearchResultModel(timeSlot: TimeSlot = TimeSlot.AM) = AppointmentSearchResult(
  appointmentSeriesId = 1,
  appointmentId = 2,
  appointmentType = AppointmentType.INDIVIDUAL,
  prisonCode = "TPR",
  appointmentName = appointmentCategorySummary().description,
  attendees = listOf(appointmentAttendeeSearchResultModel()),
  category = appointmentCategorySummary(),
  customName = null,
  internalLocation = AppointmentLocationSummary(123, UUID.fromString("44444444-1111-2222-3333-444444444444"), "TPR", "Test Appointment Location"),
  inCell = false,
  startDate = LocalDate.now().plusDays(1),
  startTime = LocalTime.of(9, 0),
  endTime = LocalTime.of(10, 30),
  timeSlot = timeSlot,
  isRepeat = false,
  sequenceNumber = 1,
  maxSequenceNumber = 1,
  isEdited = false,
  isCancelled = false,
  isExpired = false,
  createdTime = LocalDate.now().atStartOfDay(),
  updatedTime = null,
  cancelledTime = null,
  cancelledBy = null,
)

fun appointmentSetDetails(
  appointmentSetId: Long = 1,
  category: AppointmentCategorySummary = appointmentCategorySummary(),
  customName: String? = null,
  createdTime: LocalDateTime = LocalDateTime.now(),
) = AppointmentSetDetails(
  appointmentSetId,
  "TPR",
  appointmentName = if (!customName.isNullOrEmpty()) "$customName (${category.description})" else category.description,
  category,
  customName,
  AppointmentLocationSummary(123, UUID.fromString("44444444-1111-2222-3333-444444444444"), "TPR", "Test Appointment Location"),
  false,
  LocalDate.now().plusDays(1),
  appointments = listOf(
    appointmentDetails(
      1, null, AppointmentSetSummary(1, 3, 3), 1,
      listOf(
        PrisonerSummary("A1234BC", 456, "TEST01", "PRISONER01", "ACTIVE IN", "TPR", "1-2-3", "A"),
      ),
      category, customName,
      LocalTime.of(9, 0),
      LocalTime.of(10, 30), createdTime = createdTime, updatedTime = null, updatedBy = null,
      appointmentAttendeeId = 1,
    ),
    appointmentDetails(
      2, null, AppointmentSetSummary(1, 3, 3), 1,
      listOf(
        PrisonerSummary("B2345CD", 457, "TEST02", "PRISONER02", "ACTIVE IN", "TPR", "1-2-4", "E"),
      ),
      category, customName,
      LocalTime.of(9, 30),
      LocalTime.of(11, 0), createdTime = createdTime, updatedTime = null, updatedBy = null,
      appointmentAttendeeId = 2,
    ),
    appointmentDetails(
      3, null, AppointmentSetSummary(1, 3, 3), 1,
      listOf(
        PrisonerSummary("C3456DE", 458, "TEST03", "PRISONER03", "ACTIVE IN", "TPR", "1-2-5", "P"),
      ),
      category, customName,
      LocalTime.of(10, 0),
      LocalTime.of(11, 30), createdTime = createdTime, updatedTime = null, updatedBy = null,
      appointmentAttendeeId = 3,
    ),
  ),
  createdTime,
  "CREATE.USER",
  null,
  null,
)

fun appointmentAttendanceSummaryModel() = AppointmentAttendanceSummary(
  1,
  RISLEY_PRISON_CODE,
  "Friday Prayers (Chaplaincy)",
  AppointmentLocationSummary(123, UUID.fromString("44444444-1111-2222-3333-444444444444"), RISLEY_PRISON_CODE, "Chapel"),
  false,
  LocalDate.now().plusDays(1),
  LocalTime.of(12, 0),
  LocalTime.of(13, 0),
  false,
  6,
  3,
  2,
  1,
  listOf(AppointmentAttendeeSearchResult(1, "A1234BC", 456)),
)
