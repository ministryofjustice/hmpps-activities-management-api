package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import java.time.LocalDateTime

class CancelAppointmentOccurrencesJobTest {
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository))
  private val service: AppointmentCancelDomainService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = CancelAppointmentOccurrencesJob(safeJobRunner, service)

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointmentSeries = appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 4)
  private val appointmentOccurrence = appointmentSeries.appointments()[1]
  private val applyToThisAndAllFuture = appointmentSeries.applyToAppointments(appointmentOccurrence, ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES, "").toSet()

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `job type is cancel appointment occurrences`() {
    job.execute(
      appointmentSeries.appointmentSeriesId,
      appointmentOccurrence.appointmentId,
      applyToThisAndAllFuture.filterNot { it.appointmentId == appointmentOccurrence.appointmentId }.map { it.appointmentId }.toSet(),
      AppointmentOccurrenceCancelRequest(cancellationReasonId = 1),
      LocalDateTime.now(),
      "TEST.USER",
      3,
      9,
      System.currentTimeMillis(),
    )

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.CANCEL_APPOINTMENTS)
  }

  @Test
  fun `job calls cancel appointment occurrence ids`() {
    val occurrenceIdsToCancel =
      applyToThisAndAllFuture.filterNot { it.appointmentId == appointmentOccurrence.appointmentId }
        .map { it.appointmentId }.toSet()
    val request = AppointmentOccurrenceCancelRequest(cancellationReasonId = 2)
    val cancelled = LocalDateTime.now()
    val startTimeInMs = System.currentTimeMillis()

    job.execute(
      appointmentSeries.appointmentSeriesId,
      appointmentOccurrence.appointmentId,
      occurrenceIdsToCancel,
      request,
      cancelled,
      "TEST.USER",
      3,
      9,
      startTimeInMs,
    )

    verify(service).cancelAppointmentIds(
      appointmentSeries.appointmentSeriesId,
      appointmentOccurrence.appointmentId,
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
