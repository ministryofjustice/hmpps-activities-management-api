package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDateTime

class UpdateAppointmentOccurrencesJobTest {
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository))
  private val service: AppointmentOccurrenceUpdateDomainService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = UpdateAppointmentOccurrencesJob(safeJobRunner, service)

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointment = appointmentEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, repeatPeriod = AppointmentRepeatPeriod.DAILY, numberOfOccurrences = 4)
  private val appointmentOccurrence = appointment.occurrences()[1]
  private val applyToThisAndAllFuture = appointment.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES, "").toSet()

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `job type is update appointment occurrences`() {
    job.execute(
      appointment.appointmentId,
      appointmentOccurrence.appointmentOccurrenceId,
      applyToThisAndAllFuture.filterNot { it.appointmentOccurrenceId == appointmentOccurrence.appointmentOccurrenceId }.map { it.appointmentOccurrenceId }.toSet(),
      AppointmentOccurrenceUpdateRequest(internalLocationId = 456),
      emptyMap(),
      LocalDateTime.now(),
      "TEST.USER",
      3,
      9,
      System.currentTimeMillis(),
    )

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.UPDATE_APPOINTMENT_OCCURRENCES)
  }

  @Test
  fun `job calls update appointment occurrence ids`() {
    val occurrenceIdsToUpdate =
      applyToThisAndAllFuture.filterNot { it.appointmentOccurrenceId == appointmentOccurrence.appointmentOccurrenceId }
        .map { it.appointmentOccurrenceId }.toSet()
    val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456, addPrisonerNumbers = listOf("D4567EF", "E5679FG"))
    val prisonerMap = mapOf(
      "D4567EF" to PrisonerSearchPrisonerFixture.instance(prisonerNumber = "D4567EF", bookingId = 459, prisonId = "TPR"),
      "E5679FG" to PrisonerSearchPrisonerFixture.instance(prisonerNumber = "E5679FG", bookingId = 460, prisonId = "TPR"),
    )
    val updated = LocalDateTime.now()
    val startTimeInMs = System.currentTimeMillis()

    job.execute(
      appointment.appointmentId,
      appointmentOccurrence.appointmentOccurrenceId,
      occurrenceIdsToUpdate,
      request,
      prisonerMap,
      updated,
      "TEST.USER",
      3,
      9,
      startTimeInMs,
    )

    verify(service).updateAppointmentOccurrenceIds(
      appointment.appointmentId,
      appointmentOccurrence.appointmentOccurrenceId,
      occurrenceIdsToUpdate,
      request,
      prisonerMap,
      updated,
      "TEST.USER",
      3,
      9,
      startTimeInMs,
    )
  }
}
