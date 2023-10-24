package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.DailyAppointmentMetricsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduleReasonEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import java.time.LocalDate

class AppointmentMetricsJobTest {
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository, TransactionHandler()))
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val prisonApiClient: PrisonApiApplicationClient = mock()
  private val service: DailyAppointmentMetricsService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = AppointmentMetricsJob(safeJobRunner, rolloutPrisonRepository, prisonApiClient, service)

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `job type is appointment metrics`() {
    job.execute()

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.APPOINTMENTS_METRICS)
  }

  @Test
  fun `job calls service to manage appointment attendees`() {
    val rolloutPrison = rolloutPrison()
    whenever(rolloutPrisonRepository.findAll()).thenReturn(listOf(rolloutPrison))
    val appointmentCategory = appointmentCategoryReferenceCode()
    whenever(prisonApiClient.getScheduleReasons(ScheduleReasonEventType.APPOINTMENT.value)).thenReturn(listOf(appointmentCategory))

    job.execute()

    verify(service).generateAppointmentMetrics(rolloutPrison.code, appointmentCategory.code, LocalDate.now().minusDays(1))
  }
}
