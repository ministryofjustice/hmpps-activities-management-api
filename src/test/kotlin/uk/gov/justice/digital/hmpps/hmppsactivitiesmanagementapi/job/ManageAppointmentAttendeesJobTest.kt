package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAppointmentService
import java.time.LocalDate

class ManageAppointmentAttendeesJobTest {
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository))
  private val service: ManageAppointmentService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = ManageAppointmentAttendeesJob(safeJobRunner, service)

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `job type is manage appointment attendees`() {
    job.execute(1, 2)

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.MANAGE_APPOINTMENT_ATTENDEES)
  }

  @Test
  fun `job calls service to manage appointment attendees`() {
    job.execute(1, 2)

    verify(service).manageAppointmentAttendees(LocalDateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(2)))
  }
}
