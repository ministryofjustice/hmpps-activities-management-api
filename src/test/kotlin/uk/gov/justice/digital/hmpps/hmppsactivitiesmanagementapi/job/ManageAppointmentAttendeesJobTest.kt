package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.MANAGE_APPOINTMENT_ATTENDEES
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAppointmentAttendeesService

class ManageAppointmentAttendeesJobTest : JobsTestBase() {
  private val manageAppointmentAttendeesService: ManageAppointmentAttendeesService = mock()
  private val jobCaptor = argumentCaptor<Job>()

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    mockJobs(MANAGE_APPOINTMENT_ATTENDEES)
  }

  @Test
  fun `manage attendees job is triggered`() {
    val job = ManageAppointmentAttendeesJob(manageAppointmentAttendeesService, safeJobRunner)

    mockJobs(MANAGE_APPOINTMENT_ATTENDEES)

    job.execute(12)

    verify(safeJobRunner).runDistributedJob(eq(MANAGE_APPOINTMENT_ATTENDEES), any())

    verify(manageAppointmentAttendeesService).sendEvents(jobCaptor.capture(), eq(12))

    assertThat(jobCaptor.firstValue.jobType).isEqualTo(MANAGE_APPOINTMENT_ATTENDEES)
  }
}
