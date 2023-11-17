package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MonitoringService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.RolloutPrisonService
import java.time.LocalDate

class ManageAttendanceRecordsJobTest {
  private val roleOutPrisonService: RolloutPrisonService = mock {
    on { getRolloutPrisons() } doReturn listOf(
      RolloutPrisonPlan(
        moorlandPrisonCode,
        activitiesRolledOut = true,
        activitiesRolloutDate = null,
        appointmentsRolledOut = false,
        appointmentsRolloutDate = null,
      ),
      RolloutPrisonPlan(
        pentonvillePrisonCode,
        activitiesRolledOut = true,
        activitiesRolloutDate = null,
        appointmentsRolledOut = false,
        appointmentsRolloutDate = null,
      ),
    )
    on { getByPrisonCode(pentonvillePrisonCode) } doReturn
      RolloutPrisonPlan(
        pentonvillePrisonCode,
        activitiesRolledOut = true,
        activitiesRolloutDate = null,
        appointmentsRolledOut = false,
        appointmentsRolloutDate = null,
      )
  }

  private val attendancesService: ManageAttendancesService = mock()
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository, mock<MonitoringService>()))
  private val job = ManageAttendanceRecordsJob(roleOutPrisonService, attendancesService, safeJobRunner)
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()

  @Test
  fun `attendance operations triggered for single prison - without expiry`() {
    job.execute(mayBePrisonCode = pentonvillePrisonCode, withExpiry = false)

    verify(attendancesService).createAttendances(LocalDate.now(), pentonvillePrisonCode)
    verify(attendancesService, never()).expireUnmarkedAttendanceRecordsOneDayAfterTheirSession()
    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    jobDefinitionCaptor.firstValue.jobType isEqualTo JobType.ATTENDANCE_CREATE
  }

  @Test
  fun `attendance operations triggered for multiple prisons - without expiry`() {
    job.execute(withExpiry = false)

    verify(attendancesService).createAttendances(LocalDate.now(), moorlandPrisonCode)
    verify(attendancesService).createAttendances(LocalDate.now(), pentonvillePrisonCode)
    verify(attendancesService, never()).expireUnmarkedAttendanceRecordsOneDayAfterTheirSession()
    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    jobDefinitionCaptor.firstValue.jobType isEqualTo JobType.ATTENDANCE_CREATE
  }

  @Test
  fun `attendance operations triggered for multiple prisons - with expiry`() {
    job.execute(withExpiry = true)

    verify(attendancesService).createAttendances(LocalDate.now(), moorlandPrisonCode)
    verify(attendancesService).createAttendances(LocalDate.now(), pentonvillePrisonCode)
    verify(attendancesService).expireUnmarkedAttendanceRecordsOneDayAfterTheirSession()
    verify(safeJobRunner, times(2)).runJob(jobDefinitionCaptor.capture())

    jobDefinitionCaptor.firstValue.jobType isEqualTo JobType.ATTENDANCE_CREATE
    jobDefinitionCaptor.secondValue.jobType isEqualTo JobType.ATTENDANCE_EXPIRE
  }

  @Test
  fun `a future attendance date results in a no-op`() {
    job.execute(date = TimeSource.tomorrow(), withExpiry = false)

    verifyNoInteractions(attendancesService)
    verifyNoInteractions(roleOutPrisonService)
    verifyNoInteractions(safeJobRunner)
  }
}
