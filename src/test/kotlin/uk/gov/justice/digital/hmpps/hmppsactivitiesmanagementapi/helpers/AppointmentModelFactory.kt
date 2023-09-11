package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.IndividualAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentAttendeeSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentSearchResult
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun appointmentCategorySummary() =
  AppointmentCategorySummary("TEST", "Test Category")

fun appointmentSeriesModel(createdTime: LocalDateTime, updatedTime: LocalDateTime?, appointmentUpdatedTime: LocalDateTime?) =
  AppointmentSeries(
    1,
    AppointmentType.INDIVIDUAL,
    "TPR",
    "TEST",
    "Appointment description",
    123,
    false,
    LocalDate.now().plusDays(1),
    LocalTime.of(9, 0),
    LocalTime.of(10, 30),
    null,
    "Appointment series level comment",
    createdTime,
    "CREATE.USER",
    updatedTime,
    "UPDATE.USER",
    appointments = listOf(appointmentModel(createdTime, appointmentUpdatedTime)),
  )

fun appointmentAttendeeModel() =
  AppointmentAttendee(1, "A1234BC", 456, null, null, null, null, null, null, null)

fun appointmentAttendeeSearchResultModel() =
  AppointmentAttendeeSearchResult(1, "A1234BC", 456)

fun appointmentModel(createdTime: LocalDateTime = LocalDateTime.now(), updatedTime: LocalDateTime? = null) =
  Appointment(
    1,
    1,
    "TPR",
    "TEST",
    "Appointment description",
    123,
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
    attendees = listOf(appointmentAttendeeModel()),
  )

fun appointmentInstanceModel(
  createdTime: LocalDateTime = LocalDateTime.now().minusDays(1),
  updatedTime: LocalDateTime? = LocalDateTime.now(),
) = AppointmentInstance(
  3,
  1,
  2,
  3,
  AppointmentType.INDIVIDUAL,
  "TPR",
  "A1234BC",
  456,
  "TEST",
  null,
  123,
  false,
  LocalDate.now().plusDays(1),
  LocalTime.of(9, 0),
  LocalTime.of(10, 30),
  "Appointment level comment",
  created = createdTime,
  "CREATE.USER",
  updated = updatedTime,
  "UPDATE.USER",
)

fun appointmentSeriesCreateRequest(
  appointmentType: AppointmentType = AppointmentType.INDIVIDUAL,
  prisonCode: String? = "TPR",
  prisonerNumbers: List<String> = listOf("A1234BC"),
  categoryCode: String? = "TEST",
  customName: String? = "Appointment description",
  internalLocationId: Long? = 123,
  inCell: Boolean = false,
  startDate: LocalDate? = LocalDate.now().plusDays(1),
  startTime: LocalTime? = LocalTime.of(13, 0),
  endTime: LocalTime? = LocalTime.of(14, 30),
  extraInformation: String = "Appointment level comment",
  schedule: AppointmentSeriesSchedule? = null,
) =
  AppointmentSeriesCreateRequest(
    appointmentType,
    prisonCode,
    prisonerNumbers,
    categoryCode,
    customName,
    internalLocationId,
    inCell,
    startDate,
    startTime,
    endTime,
    schedule,
    extraInformation,
  )

fun appointmentSetCreateRequest(
  prisonCode: String? = "TPR",
  categoryCode: String? = "TEST",
  customName: String? = "Appointment description",
  internalLocationId: Long? = 123,
  inCell: Boolean = false,
  startDate: LocalDate? = LocalDate.now().plusDays(1),
  startTime: LocalTime? = LocalTime.of(13, 0),
  endTime: LocalTime? = LocalTime.of(14, 30),
  extraInformation: String? = "Test comment",
  prisonerNumbers: List<String?> = listOf("A1234BC", "A1234BD"),
) =
  AppointmentSetCreateRequest(
    categoryCode = categoryCode,
    prisonCode = prisonCode,
    internalLocationId = internalLocationId,
    inCell = inCell,
    startDate = startDate,
    customName = customName,
    appointments = prisonerNumbers.map {
      IndividualAppointment(
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
  internalLocationId: Long? = 123,
  startDate: LocalDate? = LocalDate.now().plusDays(1),
  startTime: LocalTime? = LocalTime.of(13, 0),
  endTime: LocalTime? = LocalTime.of(14, 30),
  extraInformation: String = "Appointment level comment",
  createdTime: LocalDateTime? = LocalDateTime.now(),
  createdBy: String? = "CREATE.USER",
  updatedTime: LocalDateTime? = null,
  updatedBy: String? = null,
  isCancelled: Boolean? = false,
) =
  AppointmentMigrateRequest(
    prisonCode,
    prisonerNumber,
    bookingId,
    categoryCode,
    internalLocationId,
    startDate,
    startTime,
    endTime,
    extraInformation,
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
  updatedBy: UserSummary? = UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
) = AppointmentSeriesDetails(
  1,
  AppointmentType.INDIVIDUAL,
  "TPR",
  if (!customName.isNullOrEmpty()) "$customName (${category.description})" else category.description,
  category = category,
  customName,
  AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
  false,
  LocalDate.now().plusDays(1),
  LocalTime.of(9, 0),
  LocalTime.of(10, 30),
  null,
  "Appointment series level comment",
  createdTime,
  UserSummary(1, "CREATE.USER", "CREATE", "USER"),
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
    ),
  ),
)

fun appointmentDetails(
  appointmentId: Long = 1,
  appointmentSeriesId: Long? = 2,
  appointmentSetSummary: AppointmentSetSummary? = null,
  sequenceNumber: Int = 3,
  prisoners: List<PrisonerSummary> = listOf(
    PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "TPR", "1-2-3"),
  ),
  category: AppointmentCategorySummary = appointmentCategorySummary(),
  customName: String? = null,
  startTime: LocalTime = LocalTime.of(9, 0),
  endTime: LocalTime = LocalTime.of(10, 30),
  extraInformation: String = "Appointment level comment",
  createdTime: LocalDateTime = LocalDateTime.now(),
  createdBy: UserSummary = UserSummary(1, "CREATE.USER", "CREATE", "USER"),
  updatedTime: LocalDateTime? = LocalDateTime.now(),
  updatedBy: UserSummary? = UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
  appointmentAttendeeId: Long = 1,
) = AppointmentDetails(
  appointmentId,
  if (appointmentSeriesId != null) AppointmentSeriesSummary(appointmentSeriesId, null, sequenceNumber, sequenceNumber) else null,
  appointmentSetSummary,
  AppointmentType.INDIVIDUAL,
  sequenceNumber,
  "TPR",
  if (!customName.isNullOrEmpty()) "$customName (${category.description})" else category.description,
  prisoners.map { AppointmentAttendeeSummary(appointmentAttendeeId, it, null) },
  category,
  customName,
  AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
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
  null,
  null,
)

fun appointmentSearchResultModel() = AppointmentSearchResult(
  1,
  2,
  AppointmentType.INDIVIDUAL,
  "TPR",
  appointmentCategorySummary().description,
  listOf(appointmentAttendeeSearchResultModel()),
  appointmentCategorySummary(),
  null,
  AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
  false,
  LocalDate.now().plusDays(1),
  LocalTime.of(9, 0),
  LocalTime.of(10, 30),
  false,
  1,
  1,
  false,
  false,
  false,
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
  AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
  false,
  LocalDate.now().plusDays(1),
  appointments = listOf(
    appointmentDetails(
      1, null, AppointmentSetSummary(1, 3, 3), 1,
      listOf(
        PrisonerSummary("A1234BC", 456, "TEST01", "PRISONER01", "TPR", "1-2-3"),
      ),
      category, customName,
      LocalTime.of(9, 0),
      LocalTime.of(10, 30), createdTime = createdTime, updatedTime = null, updatedBy = null,
      appointmentAttendeeId = 1,
    ),
    appointmentDetails(
      2, null, AppointmentSetSummary(1, 3, 3), 1,
      listOf(
        PrisonerSummary("B2345CD", 457, "TEST02", "PRISONER02", "TPR", "1-2-4"),
      ),
      category, customName,
      LocalTime.of(9, 30),
      LocalTime.of(11, 0), createdTime = createdTime, updatedTime = null, updatedBy = null,
      appointmentAttendeeId = 2,
    ),
    appointmentDetails(
      3, null, AppointmentSetSummary(1, 3, 3), 1,
      listOf(
        PrisonerSummary("C3456DE", 458, "TEST03", "PRISONER03", "TPR", "1-2-5"),
      ),
      category, customName,
      LocalTime.of(10, 0),
      LocalTime.of(11, 30), createdTime = createdTime, updatedTime = null, updatedBy = null,
      appointmentAttendeeId = 3,
    ),
  ),
  createdTime,
  UserSummary(1, "CREATE.USER", "CREATE", "USER"),
  null,
  null,
)
