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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.BulkAppointmentsRequest
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
    LocalDate.now(),
    LocalTime.of(9, 0),
    LocalTime.of(10, 30),
    "Appointment level comment",
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
    123,
    false,
    LocalDate.now(),
    LocalTime.of(9, 0),
    LocalTime.of(10, 30),
    "Appointment occurrence level comment",
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
  LocalDate.now(),
  LocalTime.of(9, 0),
  LocalTime.of(10, 30),
  "Appointment instance level comment",
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
  repeat: AppointmentRepeat? = null,
) =
  AppointmentCreateRequest(
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
  appointmentDescription: String = "Appointment description",
  prisonerNumbers: List<String> = listOf("A1234BC", "A1234BD"),
) =
  BulkAppointmentsRequest(
    categoryCode = categoryCode,
    prisonCode = prisonCode,
    internalLocationId = internalLocationId,
    inCell = inCell,
    startDate = startDate,
    appointmentDescription = appointmentDescription,
    appointments = prisonerNumbers.map {
      IndividualAppointment(
        prisonerNumber = it,
        startTime = startTime,
        endTime = endTime,
      )
    }.toList(),
  )

fun appointmentMigrateRequest(
  prisonCode: String? = "TPR",
  prisonerNumber: String = "A1234BC",
  bookingId: Long = 123,
  categoryCode: String = "TEST",
  internalLocationId: Long? = 123,
  startDate: LocalDate = LocalDate.now().plusDays(1),
  startTime: LocalTime = LocalTime.of(13, 0),
  endTime: LocalTime? = LocalTime.of(14, 30),
  comment: String = "Appointment level comment",
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
  )

fun appointmentDetails() = AppointmentDetails(
  1,
  AppointmentType.INDIVIDUAL,
  "TPR",
  prisoners = listOf(
    PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "TPR", "1-2-3"),
  ),
  appointmentCategorySummary(),
  null,
  AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
  false,
  LocalDate.now(),
  LocalTime.of(9, 0),
  LocalTime.of(10, 30),
  null,
  "Appointment level comment",
  LocalDateTime.now(),
  UserSummary(1, "CREATE.USER", "CREATE", "USER"),
  LocalDateTime.now(),
  UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
  occurrences = listOf(
    AppointmentOccurrenceSummary(
      1,
      1,
      1,
      AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
      false,
      LocalDate.now(),
      LocalTime.of(9, 0),
      LocalTime.of(10, 30),
      "Appointment occurrence level comment",
      isEdited = false,
      isCancelled = false,
      LocalDateTime.now(),
      UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
    ),
  ),
)

fun appointmentOccurrenceDetails() = AppointmentOccurrenceDetails(
  1,
  2,
  AppointmentType.INDIVIDUAL,
  3,
  "TPR",
  prisoners = listOf(
    PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "TPR", "1-2-3"),
  ),
  appointmentCategorySummary(),
  null,
  AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
  false,
  LocalDate.now(),
  LocalTime.of(9, 0),
  LocalTime.of(10, 30),
  "Appointment level comment",
  null,
  false,
  false,
  LocalDateTime.now(),
  UserSummary(1, "CREATE.USER", "CREATE", "USER"),
  LocalDateTime.now(),
  UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
)

fun appointmentOccurrenceSearchResultModel() = AppointmentOccurrenceSearchResult(
  1,
  2,
  AppointmentType.INDIVIDUAL,
  "TPR",
  listOf(appointmentOccurrenceAllocationModel()),
  appointmentCategorySummary(),
  null,
  AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
  false,
  LocalDate.now(),
  LocalTime.of(9, 0),
  LocalTime.of(10, 30),
  false,
  1,
  1,
  false,
  false,
)
