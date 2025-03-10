package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ATTENDANCE_CREATE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ATTENDANCE_EXPIRE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.Clock
import java.time.LocalDate

class ManageAttendanceRecordsJobTest : JobsTestBase() {
  private val rollOutPrisonService: RolloutPrisonService = mock {
    on { getRolloutPrisons() } doReturn listOf(
      RolloutPrisonPlan(
        MOORLAND_PRISON_CODE,
        activitiesRolledOut = true,
        appointmentsRolledOut = false,
        prisonLive = true,
      ),
      RolloutPrisonPlan(
        PENTONVILLE_PRISON_CODE,
        activitiesRolledOut = true,
        appointmentsRolledOut = false,
        prisonLive = true,
      ),
    )
    on { getByPrisonCode(PENTONVILLE_PRISON_CODE) } doReturn
      RolloutPrisonPlan(
        PENTONVILLE_PRISON_CODE,
        activitiesRolledOut = true,
        appointmentsRolledOut = false,
        prisonLive = true,
      )
  }

  private val attendancesService: ManageAttendancesService = mock()
  private val job = ManageAttendanceRecordsJob(rollOutPrisonService, attendancesService, safeJobRunner, Clock.systemDefaultZone())
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()

  @Test
  fun `attendance operations triggered for single prison - without expiry`() {
    mockJobs(ATTENDANCE_CREATE)

    job.execute(mayBePrisonCode = PENTONVILLE_PRISON_CODE, withExpiry = false)

    verify(attendancesService).createAttendances(LocalDate.now(), PENTONVILLE_PRISON_CODE)
    verify(attendancesService, never()).expireUnmarkedAttendanceRecordsOneDayAfterTheirSession()
    verify(safeJobRunner).runJobWithRetry(jobDefinitionCaptor.capture())

    jobDefinitionCaptor.firstValue.jobType isEqualTo ATTENDANCE_CREATE
  }

  @Test
  fun `attendance operations triggered for multiple prisons - without expiry`() {
    mockJobs(ATTENDANCE_CREATE)

    job.execute(withExpiry = false)

    verify(attendancesService).createAttendances(LocalDate.now(), MOORLAND_PRISON_CODE)
    verify(attendancesService).createAttendances(LocalDate.now(), PENTONVILLE_PRISON_CODE)
    verify(attendancesService, never()).expireUnmarkedAttendanceRecordsOneDayAfterTheirSession()
    verify(safeJobRunner).runJobWithRetry(jobDefinitionCaptor.capture())

    jobDefinitionCaptor.firstValue.jobType isEqualTo ATTENDANCE_CREATE
  }

  @Test
  fun `attendance operations triggered for multiple prisons - with expiry`() {
    mockJobs(ATTENDANCE_CREATE, ATTENDANCE_EXPIRE)

    job.execute(withExpiry = true)

    verify(attendancesService).createAttendances(LocalDate.now(), MOORLAND_PRISON_CODE)
    verify(attendancesService).createAttendances(LocalDate.now(), PENTONVILLE_PRISON_CODE)
    verify(attendancesService).expireUnmarkedAttendanceRecordsOneDayAfterTheirSession()
    verify(safeJobRunner).runJobWithRetry(jobDefinitionCaptor.capture())
    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    jobDefinitionCaptor.firstValue.jobType isEqualTo ATTENDANCE_CREATE
    jobDefinitionCaptor.secondValue.jobType isEqualTo ATTENDANCE_EXPIRE
  }

  @Test
  fun `a future attendance date results in a no-op`() {
    job.execute(date = TimeSource.tomorrow(), withExpiry = false)

    verifyNoInteractions(attendancesService)
    verifyNoInteractions(rollOutPrisonService)
    verifyNoInteractions(safeJobRunner)
  }
}
