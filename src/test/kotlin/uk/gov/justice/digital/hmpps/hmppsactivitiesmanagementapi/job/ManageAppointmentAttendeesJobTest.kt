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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentAttendeeService

class ManageAppointmentAttendeesJobTest : JobsTestBase() {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val service: AppointmentAttendeeService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = ManageAppointmentAttendeesJob(safeJobRunner, rolloutPrisonRepository, service)

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    mockJobs(JobType.MANAGE_APPOINTMENT_ATTENDEES)
  }

  @Test
  fun `job type is manage appointment attendees`() {
    job.execute(1)

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.MANAGE_APPOINTMENT_ATTENDEES)
  }

  @Test
  fun `job calls service to manage appointment attendees`() {
    val rolloutPrison = rolloutPrison()
    whenever(rolloutPrisonRepository.findAll()).thenReturn(listOf(rolloutPrison))

    job.execute(1)

    verify(service).manageAppointmentAttendees(rolloutPrison.code, 1)
  }
}
