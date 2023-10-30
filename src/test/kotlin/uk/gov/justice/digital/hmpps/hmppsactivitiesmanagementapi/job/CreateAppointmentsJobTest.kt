package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCreateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.CREATE_APPOINTMENTS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository

class CreateAppointmentsJobTest {
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository))
  private val appointmentCreateDomainService: AppointmentCreateDomainService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = CreateAppointmentsJob(safeJobRunner, appointmentCreateDomainService)

  private val appointmentSeriesId = 1L
  private val prisonNumberBookingIdMap = mapOf("A1234BC" to 1L)
  private val startTimeInMs = 123L
  private val categoryDescription = "Category description"
  private val locationDescription = "Location description"

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `job type is create appointments`() {
    job.execute(appointmentSeriesId, prisonNumberBookingIdMap, startTimeInMs, categoryDescription, locationDescription)

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(CREATE_APPOINTMENTS)
  }

  @Test
  fun `job calls create appointments`() {
    job.execute(appointmentSeriesId, prisonNumberBookingIdMap, startTimeInMs, categoryDescription, locationDescription)

    verify(appointmentCreateDomainService).createAppointments(appointmentSeriesId, prisonNumberBookingIdMap, startTimeInMs, categoryDescription, locationDescription)
  }
}
