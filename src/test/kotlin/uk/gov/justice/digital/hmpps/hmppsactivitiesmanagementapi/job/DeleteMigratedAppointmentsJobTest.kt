package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.DELETE_MIGRATED_APPOINTMENTS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.MigrateAppointmentService
import java.time.LocalDate

class DeleteMigratedAppointmentsJobTest : JobsTestBase() {
  private val service: MigrateAppointmentService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = DeleteMigratedAppointmentsJob(safeJobRunner, service)

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    mockJobs(DELETE_MIGRATED_APPOINTMENTS)
  }

  @Test
  fun `job type is delete migrated appointments`() {
    job.execute("RSI", LocalDate.now())

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(DELETE_MIGRATED_APPOINTMENTS)
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
