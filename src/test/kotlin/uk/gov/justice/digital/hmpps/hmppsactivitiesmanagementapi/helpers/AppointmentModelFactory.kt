package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentCategory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun appointmentCategoryModel() =
  AppointmentCategory(1, null, "TEST", "Test Category", true, 2)

fun appointmentCategorySummary() =
  AppointmentCategorySummary(1, "TEST", "Test Category")

fun appointmentModel(created: LocalDateTime, updated: LocalDateTime?, occurrenceUpdated: LocalDateTime?) =
  Appointment(
    1,
    appointmentCategoryModel(),
    "TPR",
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
    123,
    false,
    LocalDate.now(),
    LocalTime.of(9, 0),
    LocalTime.of(10, 30),
    "Appointment occurrence level comment",
    false,
    updated,
    "UPDATE.USER",
    allocations = listOf(appointmentOccurrenceAllocationModel()),
    instances = listOf(appointmentInstanceModel()),
  )

fun appointmentInstanceModel() =
  AppointmentInstance(
    1,
    appointmentCategoryModel(),
    "TPR",
    123,
    false,
    "A1234BC",
    456,
    LocalDate.now(),
    LocalTime.of(9, 0),
    LocalTime.of(10, 30),
    "Appointment instance level comment",
    attended = true,
    cancelled = false,
  )

fun appointmentCreateRequest(
  categoryId: Long? = 1,
  prisonCode: String? = "TPR",
  internalLocationId: Long? = 123,
  inCell: Boolean = false,
  startDate: LocalDate? = LocalDate.now().plusDays(1),
  startTime: LocalTime? = LocalTime.of(13, 0),
  endTime: LocalTime? = LocalTime.of(14, 30),
  comment: String = "Appointment level comment",
  prisonerNumbers: List<String> = listOf("A1234BC"),
) =
  AppointmentCreateRequest(
    categoryId,
    prisonCode,
    internalLocationId,
    inCell,
    startDate,
    startTime,
    endTime,
    comment,
    prisonerNumbers,
  )

fun appointmentDetail() = AppointmentDetail(
  1,
  appointmentCategorySummary(),
  "TPR",
  AppointmentLocationSummary(123, "TPR", "Test Appointment Location"),
  false,
  LocalDate.now(),
  LocalTime.of(9, 0),
  LocalTime.of(10, 30),
  "Appointment level comment",
  LocalDateTime.now(),
  UserSummary(1, "CREATE.USER", "CREATE", "USER"),
  LocalDateTime.now(),
  UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
  occurrences = listOf(
    AppointmentOccurrenceSummary(
      1,
      AppointmentLocationSummary(123, "TPR", "Test Appointment Location"),
      false,
      LocalDate.now(),
      LocalTime.of(9, 0),
      LocalTime.of(10, 30),
      "Appointment occurrence level comment",
      isEdited = false,
      isCancelled = false,
      LocalDateTime.now(),
      UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
      1,
    ),
  ),
  prisoners = listOf(
    PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "TPR", "1-2-3"),
  ),
)
