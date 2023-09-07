package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import java.time.LocalDateTime

class CancelAppointmentOccurrencesJobTest {
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository))
  private val service: AppointmentOccurrenceCancelDomainService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = CancelAppointmentOccurrencesJob(safeJobRunner, service)

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointmentSeries = appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, repeatPeriod = AppointmentRepeatPeriod.DAILY, numberOfOccurrences = 4)
  private val appointmentOccurrence = appointmentSeries.occurrences()[1]
  private val applyToThisAndAllFuture = appointmentSeries.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES, "").toSet()

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `job type is cancel appointment occurrences`() {
    job.execute(
      appointmentSeries.appointmentSeriesId,
      appointmentOccurrence.appointmentOccurrenceId,
      applyToThisAndAllFuture.filterNot { it.appointmentOccurrenceId == appointmentOccurrence.appointmentOccurrenceId }.map { it.appointmentOccurrenceId }.toSet(),
      AppointmentOccurrenceCancelRequest(cancellationReasonId = 1),
      LocalDateTime.now(),
      "TEST.USER",
      3,
      9,
      System.currentTimeMillis(),
    )

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.CANCEL_APPOINTMENT_OCCURRENCES)
  }

  @Test
  fun `job calls cancel appointment occurrence ids`() {
    val occurrenceIdsToCancel =
      applyToThisAndAllFuture.filterNot { it.appointmentOccurrenceId == appointmentOccurrence.appointmentOccurrenceId }
        .map { it.appointmentOccurrenceId }.toSet()
    val request = AppointmentOccurrenceCancelRequest(cancellationReasonId = 2)
    val cancelled = LocalDateTime.now()
    val startTimeInMs = System.currentTimeMillis()

    job.execute(
      appointmentSeries.appointmentSeriesId,
      appointmentOccurrence.appointmentOccurrenceId,
      occurrenceIdsToCancel,
      request,
      cancelled,
      "TEST.USER",
      3,
      9,
      startTimeInMs,
    )

    verify(service).cancelAppointmentOccurrenceIds(
      appointmentSeries.appointmentSeriesId,
      appointmentOccurrence.appointmentOccurrenceId,
      occurrenceIdsToCancel,
      request,
      cancelled,
      "TEST.USER",
      3,
      9,
      startTimeInMs,
    )
  }
}
