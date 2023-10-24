package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendanceOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler

class ManageAttendanceRecordsJobTest {
  private val attendancesService: ManageAttendancesService = mock()
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository, TransactionHandler()))
  private val job = ManageAttendanceRecordsJob(attendancesService, safeJobRunner)
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()

  @Test
  fun `attendance operations triggered - without expiry`() {
    job.execute(withExpiry = false)

    verify(attendancesService).attendances(AttendanceOperation.CREATE)
    verify(attendancesService, never()).attendances(AttendanceOperation.EXPIRE)
    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.ATTENDANCE_CREATE)
  }

  @Test
  fun `attendance operations triggered - with expiry`() {
    job.execute(withExpiry = true)

    verify(attendancesService).attendances(AttendanceOperation.CREATE)
    verify(attendancesService).attendances(AttendanceOperation.EXPIRE)
    verify(safeJobRunner, times(2)).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.ATTENDANCE_CREATE)
    assertThat(jobDefinitionCaptor.secondValue.jobType).isEqualTo(JobType.ATTENDANCE_EXPIRE)
  }
}
