package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.DailyAppointmentMetricsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.LocalDate

class AppointmentMetricsJobTest : JobsTestBase() {
  private val rolloutPrisonRepository: RolloutPrisonService = mock()
  private val appointmentCategoryRepository: AppointmentCategoryRepository = mock()
  private val service: DailyAppointmentMetricsService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = AppointmentMetricsJob(safeJobRunner, rolloutPrisonRepository, appointmentCategoryRepository, service)

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    mockJobs(JobType.APPOINTMENTS_METRICS)
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
    whenever(rolloutPrisonRepository.getRolloutPrisons(true)).thenReturn(listOf(rolloutPrison))
    val appointmentCategory = appointmentCategory()
    whenever(appointmentCategoryRepository.findAll()).thenReturn(listOf(appointmentCategory))

    job.execute()

    verify(service).generateAppointmentMetrics(rolloutPrison.prisonCode, appointmentCategory.code, LocalDate.now().minusDays(1))
  }
}
