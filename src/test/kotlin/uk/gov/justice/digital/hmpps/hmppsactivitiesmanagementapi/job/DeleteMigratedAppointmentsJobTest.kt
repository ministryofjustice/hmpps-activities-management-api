package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MigrateAppointmentService
import java.time.LocalDate

class DeleteMigratedAppointmentsJobTest {
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository))
  private val service: MigrateAppointmentService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = DeleteMigratedAppointmentsJob(safeJobRunner, service)

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `job type is delete migrated appointments`() {
    job.execute("RSI", LocalDate.now())

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.DELETE_MIGRATED_APPOINTMENTS)
  }

  @Test
  fun `job calls service to delete migrated appointments`() {
    job.execute("RSI", LocalDate.now())

    verify(service).deleteMigratedAppointments("RSI", LocalDate.now())
  }

  @Test
  fun `job calls service to delete migrated appointments with optional category code`() {
    job.execute("RSI", LocalDate.now(), "CHAP")

    verify(service).deleteMigratedAppointments("RSI", LocalDate.now(), "CHAP")
  }
}
