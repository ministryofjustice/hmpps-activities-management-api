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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun appointmentCategorySummary() =
  AppointmentCategorySummary("TEST", "Test Category")

fun appointmentModel(created: LocalDateTime, updated: LocalDateTime?, occurrenceUpdated: LocalDateTime?) =
  Appointment(
    1,
    "TEST",
    "TPR",
    123,
    false,
    LocalDate.now(),
    LocalTime.of(9, 0),
    LocalTime.of(10, 30),
    AppointmentType.INDIVIDUAL,
    "Appointment level comment",
    "Appointment description",
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
  )

fun appointmentInstanceModel(
  created: LocalDateTime = LocalDateTime.now().minusDays(1),
  updated: LocalDateTime? = LocalDateTime.now(),
) = AppointmentInstance(
  3,
  1,
  2,
  3,
  "TEST",
  "TPR",
  123,
  false,
  "A1234BC",
  456,
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
  categoryCode: String? = "TEST",
  prisonCode: String? = "TPR",
  internalLocationId: Long? = 123,
  inCell: Boolean = false,
  startDate: LocalDate? = LocalDate.now().plusDays(1),
  startTime: LocalTime? = LocalTime.of(13, 0),
  endTime: LocalTime? = LocalTime.of(14, 30),
  appointmentType: AppointmentType = AppointmentType.INDIVIDUAL,
  comment: String = "Appointment level comment",
  appointmentDescription: String? = "Appointment description",
  repeat: AppointmentRepeat? = null,
  prisonerNumbers: List<String> = listOf("A1234BC"),
) =
  AppointmentCreateRequest(
    categoryCode,
    prisonCode,
    internalLocationId,
    inCell,
    startDate,
    startTime,
    endTime,
    appointmentType,
    repeat,
    comment,
    appointmentDescription,
    prisonerNumbers,
  )

fun appointmentDetails() = AppointmentDetails(
  1,
  appointmentCategorySummary(),
  "TPR",
  AppointmentLocationSummary(123, "TPR", "Test Appointment Location"),
  false,
  LocalDate.now(),
  LocalTime.of(9, 0),
  LocalTime.of(10, 30),
  null,
  AppointmentType.INDIVIDUAL,
  "Appointment level comment",
  LocalDateTime.now(),
  UserSummary(1, "CREATE.USER", "CREATE", "USER"),
  LocalDateTime.now(),
  UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
  occurrences = listOf(
    AppointmentOccurrenceSummary(
      1,
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

fun appointmentOccurrenceDetails() = AppointmentOccurrenceDetails(
  1,
  2,
  3,
  appointmentCategorySummary(),
  "TPR",
  AppointmentLocationSummary(123, "TPR", "Test Appointment Location"),
  false,
  LocalDate.now(),
  LocalTime.of(9, 0),
  LocalTime.of(10, 30),
  AppointmentType.INDIVIDUAL,
  "Appointment level comment",
  false,
  false,
  LocalDateTime.now(),
  UserSummary(1, "CREATE.USER", "CREATE", "USER"),
  LocalDateTime.now(),
  UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
  prisoners = listOf(
    PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "TPR", "1-2-3"),
  ),
)
