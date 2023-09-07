package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointmentSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.IndividualAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentOccurrenceSearchResult
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun appointmentCategorySummary() =
  AppointmentCategorySummary("TEST", "Test Category")

fun appointmentModel(created: LocalDateTime, updated: LocalDateTime?, occurrenceUpdated: LocalDateTime?) =
  Appointment(
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
    created,
    "CREATE.USER",
    updated,
    "UPDATE.USER",
    occurrences = listOf(appointmentOccurrenceModel(occurrenceUpdated)),
  )

fun appointmentOccurrenceAllocationModel() =
  AppointmentOccurrenceAllocation(1, "A1234BC", 456)

fun appointmentOccurrenceModel(updated: LocalDateTime?) =
  AppointmentOccurrence(
    1,
    1,
    "TEST",
    "Appointment description",
    123,
    false,
    LocalDate.now().plusDays(1),
    LocalTime.of(9, 0),
    LocalTime.of(10, 30),
    "Appointment level comment",
    null,
    null,
    null,
    updated,
    "UPDATE.USER",
    allocations = listOf(appointmentOccurrenceAllocationModel()),
  )

fun appointmentInstanceModel(
  created: LocalDateTime = LocalDateTime.now().minusDays(1),
  updated: LocalDateTime? = LocalDateTime.now(),
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
  created = created,
  "CREATE.USER",
  updated = updated,
  "UPDATE.USER",
)

fun appointmentCreateRequest(
  appointmentType: AppointmentType = AppointmentType.INDIVIDUAL,
  prisonCode: String? = "TPR",
  prisonerNumbers: List<String> = listOf("A1234BC"),
  categoryCode: String? = "TEST",
  appointmentDescription: String? = "Appointment description",
  internalLocationId: Long? = 123,
  inCell: Boolean = false,
  startDate: LocalDate? = LocalDate.now().plusDays(1),
  startTime: LocalTime? = LocalTime.of(13, 0),
  endTime: LocalTime? = LocalTime.of(14, 30),
  comment: String = "Appointment level comment",
  repeat: AppointmentSchedule? = null,
) =
  AppointmentSeriesCreateRequest(
    appointmentType,
    prisonCode,
    prisonerNumbers,
    categoryCode,
    appointmentDescription,
    internalLocationId,
    inCell,
    startDate,
    startTime,
    endTime,
    repeat,
    comment,
  )

fun bulkAppointmentRequest(
  categoryCode: String = "TEST",
  prisonCode: String = "TPR",
  internalLocationId: Long = 123,
  inCell: Boolean = false,
  startDate: LocalDate = LocalDate.now().plusDays(1),
  startTime: LocalTime = LocalTime.of(13, 0),
  endTime: LocalTime = LocalTime.of(14, 30),
  comment: String = "Test comment",
  appointmentDescription: String = "Appointment description",
  prisonerNumbers: List<String> = listOf("A1234BC", "A1234BD"),
) =
  AppointmentSetCreateRequest(
    categoryCode = categoryCode,
    prisonCode = prisonCode,
    internalLocationId = internalLocationId,
    inCell = inCell,
    startDate = startDate,
    customName = appointmentDescription,
    appointments = prisonerNumbers.map {
      IndividualAppointment(
        prisonerNumber = it,
        startTime = startTime,
        endTime = endTime,
        extraInformation = comment,
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
  comment: String = "Appointment level comment",
  created: LocalDateTime? = LocalDateTime.now(),
  createdBy: String? = "CREATE.USER",
  updated: LocalDateTime? = null,
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
    comment,
    isCancelled,
    created,
    createdBy,
    updated,
    updatedBy,
  )

fun appointmentDetails(
  appointmentDescription: String? = null,
  category: AppointmentCategorySummary = appointmentCategorySummary(),
  created: LocalDateTime = LocalDateTime.now(),
  updated: LocalDateTime? = LocalDateTime.now(),
  updatedBy: UserSummary? = UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
) = AppointmentDetails(
  1,
  AppointmentType.INDIVIDUAL,
  "TPR",
  if (!appointmentDescription.isNullOrEmpty()) "$appointmentDescription (${category.description})" else category.description,
  prisoners = listOf(
    PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "TPR", "1-2-3"),
  ),
  category = category,
  appointmentDescription,
  AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
  false,
  LocalDate.now().plusDays(1),
  LocalTime.of(9, 0),
  LocalTime.of(10, 30),
  null,
  "Appointment series level comment",
  created,
  UserSummary(1, "CREATE.USER", "CREATE", "USER"),
  updated,
  updatedBy,
  occurrences = listOf(
    AppointmentOccurrenceSummary(
      1,
      1,
      if (!appointmentDescription.isNullOrEmpty()) "$appointmentDescription (${category.description})" else category.description,
      category = category,
      appointmentDescription,
      AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
      false,
      LocalDate.now().plusDays(1),
      LocalTime.of(9, 0),
      LocalTime.of(10, 30),
      "Appointment level comment",
      isEdited = updated != null,
      isCancelled = false,
      updated,
      updatedBy,
    ),
  ),
)

fun appointmentOccurrenceDetails(
  appointmentOccurrenceId: Long = 1,
  appointmentId: Long = 2,
  bulkAppointmentSummary: BulkAppointmentSummary? = null,
  sequenceNumber: Int = 3,
  prisoners: List<PrisonerSummary> = listOf(
    PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "TPR", "1-2-3"),
  ),
  category: AppointmentCategorySummary = appointmentCategorySummary(),
  appointmentDescription: String? = null,
  startTime: LocalTime = LocalTime.of(9, 0),
  endTime: LocalTime = LocalTime.of(10, 30),
  comment: String = "Appointment level comment",
  created: LocalDateTime = LocalDateTime.now(),
  createdBy: UserSummary = UserSummary(1, "CREATE.USER", "CREATE", "USER"),
  updated: LocalDateTime? = LocalDateTime.now(),
  updatedBy: UserSummary? = UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
) = AppointmentOccurrenceDetails(
  appointmentOccurrenceId,
  appointmentId,
  bulkAppointmentSummary,
  AppointmentType.INDIVIDUAL,
  sequenceNumber,
  "TPR",
  if (!appointmentDescription.isNullOrEmpty()) "$appointmentDescription (${category.description})" else category.description,
  prisoners,
  category,
  appointmentDescription,
  AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
  false,
  LocalDate.now().plusDays(1),
  startTime,
  endTime,
  comment,
  null,
  updated != null,
  false,
  false,
  created,
  createdBy,
  updated,
  updatedBy,
  null,
  null,
)

fun appointmentOccurrenceSearchResultModel() = AppointmentOccurrenceSearchResult(
  1,
  2,
  AppointmentType.INDIVIDUAL,
  "TPR",
  appointmentCategorySummary().description,
  listOf(appointmentOccurrenceAllocationModel()),
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

fun bulkAppointmentDetails(
  bulkAppointmentId: Long = 1,
  category: AppointmentCategorySummary = appointmentCategorySummary(),
  appointmentDescription: String? = null,
  created: LocalDateTime = LocalDateTime.now(),
) = BulkAppointmentDetails(
  bulkAppointmentId,
  "TPR",
  appointmentName = if (!appointmentDescription.isNullOrEmpty()) "$appointmentDescription (${category.description})" else category.description,
  category,
  appointmentDescription,
  AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
  false,
  LocalDate.now().plusDays(1),
  occurrences = listOf(
    appointmentOccurrenceDetails(
      1, 1, BulkAppointmentSummary(1, 3), 1,
      listOf(
        PrisonerSummary("A1234BC", 456, "TEST01", "PRISONER01", "TPR", "1-2-3"),
      ),
      category, appointmentDescription,
      LocalTime.of(9, 0),
      LocalTime.of(10, 30), created = created, updated = null, updatedBy = null,
    ),
    appointmentOccurrenceDetails(
      2, 2, BulkAppointmentSummary(1, 3), 1,
      listOf(
        PrisonerSummary("B2345CD", 457, "TEST02", "PRISONER02", "TPR", "1-2-4"),
      ),
      category, appointmentDescription,
      LocalTime.of(9, 30),
      LocalTime.of(11, 0), created = created, updated = null, updatedBy = null,
    ),
    appointmentOccurrenceDetails(
      3, 3, BulkAppointmentSummary(1, 3), 1,
      listOf(
        PrisonerSummary("C3456DE", 458, "TEST03", "PRISONER03", "TPR", "1-2-5"),
      ),
      category, appointmentDescription,
      LocalTime.of(10, 0),
      LocalTime.of(11, 30), created = created, updated = null, updatedBy = null,
    ),
  ),
  created,
  UserSummary(1, "CREATE.USER", "CREATE", "USER"),
)
