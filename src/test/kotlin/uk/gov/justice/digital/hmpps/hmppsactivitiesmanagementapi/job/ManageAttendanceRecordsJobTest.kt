package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature.JOBS_SQS_MANAGE_ATTENDANCES_ENABLED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ATTENDANCE_CREATE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ATTENDANCE_EXPIRE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ExpireAttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageNewAttendancesService
import java.time.Clock
import java.time.LocalDate

class ManageAttendanceRecordsJobTest : JobsTestBase() {

  private val manageNewAttendancesService: ManageNewAttendancesService = mock()
  private val expireAttendancesService: ExpireAttendancesService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val featureSwitches: FeatureSwitches = mock()

  private val jobNoSQS = ManageAttendanceRecordsJob(
    manageNewAttendancesService,
    expireAttendancesService,
    safeJobRunner,
    Clock.systemDefaultZone(),
    featureSwitches,
  )
  private lateinit var jobWithSQS: ManageAttendanceRecordsJob

  private val captor = argumentCaptor<Job>()

  @BeforeEach
  fun setUp() {
    val featureSwitches: FeatureSwitches = mock()

    whenever(featureSwitches.isEnabled(JOBS_SQS_MANAGE_ATTENDANCES_ENABLED)).thenReturn(true)

    jobWithSQS = ManageAttendanceRecordsJob(
      manageNewAttendancesService,
      expireAttendancesService,
      safeJobRunner,
      Clock.systemDefaultZone(),
      featureSwitches,
    )
  }

  @Test
  fun `attendance operations triggered for single prison - without expiry - without SQS`() {
    mockJobs(ATTENDANCE_CREATE)

    jobNoSQS.execute(mayBePrisonCode = PENTONVILLE_PRISON_CODE, withExpiry = false)

    verify(manageNewAttendancesService).createAttendances(LocalDate.now(), PENTONVILLE_PRISON_CODE)
    verify(safeJobRunner).runJobWithRetry(jobDefinitionCaptor.capture())

    jobDefinitionCaptor.firstValue.jobType isEqualTo ATTENDANCE_CREATE

    verifyNoMoreInteractions(safeJobRunner)
    verifyNoMoreInteractions(manageNewAttendancesService)
    verifyNoInteractions(expireAttendancesService)
  }

  @Test
  fun `attendance operations triggered for single prison - without expiry - with SQS`() {
    mockJobs(ATTENDANCE_CREATE)

    jobWithSQS.execute(mayBePrisonCode = PENTONVILLE_PRISON_CODE, withExpiry = false)

    verify(safeJobRunner).runDistributedJob(eq(ATTENDANCE_CREATE), any())
    verify(manageNewAttendancesService).sendEvents(captor.capture(), eq(LocalDate.now()), eq(PENTONVILLE_PRISON_CODE), eq(false))
    assertThat(captor.firstValue.jobType).isEqualTo(ATTENDANCE_CREATE)

    verifyNoMoreInteractions(safeJobRunner)
    verifyNoMoreInteractions(manageNewAttendancesService)
    verifyNoInteractions(expireAttendancesService)
  }

  @Test
  fun `attendance operations triggered for single prison - with expiry - without SQS`() {
    mockJobs(ATTENDANCE_CREATE, ATTENDANCE_EXPIRE)

    jobNoSQS.execute(mayBePrisonCode = PENTONVILLE_PRISON_CODE, withExpiry = true)

    verify(manageNewAttendancesService).createAttendances(LocalDate.now(), PENTONVILLE_PRISON_CODE)
    verify(expireAttendancesService).expireUnmarkedAttendances()
    verify(safeJobRunner).runJobWithRetry(jobDefinitionCaptor.capture())
    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    jobDefinitionCaptor.firstValue.jobType isEqualTo ATTENDANCE_CREATE
    jobDefinitionCaptor.secondValue.jobType isEqualTo ATTENDANCE_EXPIRE

    verifyNoMoreInteractions(safeJobRunner)
    verifyNoMoreInteractions(manageNewAttendancesService)
    verifyNoMoreInteractions(expireAttendancesService)
  }

  @Test
  fun `attendance operations triggered for single prison - with expiry - with SQS`() {
    mockJobs(ATTENDANCE_CREATE)

    jobWithSQS.execute(mayBePrisonCode = PENTONVILLE_PRISON_CODE, withExpiry = true)

    verify(safeJobRunner).runDistributedJob(eq(ATTENDANCE_CREATE), any())
    verify(manageNewAttendancesService).sendEvents(captor.capture(), eq(LocalDate.now()), eq(PENTONVILLE_PRISON_CODE), eq(true))
    assertThat(captor.firstValue.jobType).isEqualTo(ATTENDANCE_CREATE)

    verifyNoMoreInteractions(safeJobRunner)
    verifyNoMoreInteractions(manageNewAttendancesService)
    verifyNoInteractions(expireAttendancesService)
  }

  @Test
  fun `a future attendance date results in a no-op`() {
    jobNoSQS.execute(date = TimeSource.tomorrow(), withExpiry = false)

    verifyNoInteractions(safeJobRunner)
    verifyNoInteractions(manageNewAttendancesService)
  }
}
