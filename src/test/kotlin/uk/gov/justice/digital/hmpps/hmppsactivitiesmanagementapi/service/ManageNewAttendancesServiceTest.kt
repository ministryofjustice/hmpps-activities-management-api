package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ATTENDANCE_CREATE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ATTENDANCE_EXPIRE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobEventMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsSqsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsTestBase
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.NewActivityAttendanceJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.LocalDate

class ManageNewAttendancesServiceTest : JobsTestBase() {
  private val rolloutPrisonService: RolloutPrisonService = mock {
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
      RolloutPrisonPlan(
        RISLEY_PRISON_CODE,
        activitiesRolledOut = false,
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

  private val manageAttendancesService: ManageAttendancesService = mock()
  private val expireAttendancesService: ExpireAttendancesService = mock()
  private val jobsSqsService: JobsSqsService = mock()
  private val jobService: JobService = mock()

  val service = ManageNewAttendancesService(
    rolloutPrisonService,
    manageAttendancesService,
    expireAttendancesService,
    jobsSqsService,
    jobService,
    safeJobRunner,
  )

  private val today = LocalDate.now()
  private val nextJobCaptor = argumentCaptor<Job>()

  @Test
  fun `should send events to queue for all prisons`() {
    service.sendEvents(Job(123, ATTENDANCE_CREATE), today, null, false)

    verify(jobService).initialiseCounts(123, 2)

    verify(jobsSqsService).sendJobEvent(
      JobEventMessage(
        123,
        ATTENDANCE_CREATE,
        NewActivityAttendanceJobEvent(PENTONVILLE_PRISON_CODE, today, false),
      ),
    )
    verify(jobsSqsService).sendJobEvent(
      JobEventMessage(
        123,
        ATTENDANCE_CREATE,
        NewActivityAttendanceJobEvent(MOORLAND_PRISON_CODE, today, false),
      ),
    )

    verifyNoMoreInteractions(jobsSqsService)
  }

  @Test
  fun `should send events to queue for one prisons`() {
    service.sendEvents(Job(123, ATTENDANCE_CREATE), today, PENTONVILLE_PRISON_CODE, false)

    verify(jobService).initialiseCounts(123, 1)

    verify(jobsSqsService).sendJobEvent(
      JobEventMessage(
        123,
        ATTENDANCE_CREATE,
        NewActivityAttendanceJobEvent(PENTONVILLE_PRISON_CODE, today, false),
      ),
    )

    verifyNoMoreInteractions(jobsSqsService)
  }

  @Test
  fun `handles event when not finished and not expiring unmarked attendances`() {
    service.handleEvent(123, PENTONVILLE_PRISON_CODE, today, false)

    verify(manageAttendancesService).createAttendances(today, PENTONVILLE_PRISON_CODE)

    verifyNoInteractions(safeJobRunner)
    verifyNoInteractions(expireAttendancesService)
  }

  @Test
  fun `handles event when not finished and expiring unmarked attendances`() {
    service.handleEvent(123, PENTONVILLE_PRISON_CODE, today, true)

    verify(manageAttendancesService).createAttendances(today, PENTONVILLE_PRISON_CODE)

    verifyNoInteractions(safeJobRunner)
    verifyNoInteractions(expireAttendancesService)
  }

  @Test
  fun `handles event when finished and not expiring unmarked attendances`() {
    whenever(jobService.incrementCount(123L)).thenReturn(true)

    service.handleEvent(123, PENTONVILLE_PRISON_CODE, today, false)

    verify(manageAttendancesService).createAttendances(today, PENTONVILLE_PRISON_CODE)

    verifyNoInteractions(safeJobRunner)
    verifyNoInteractions(expireAttendancesService)
  }

  @Test
  fun `handles event when finished and expiring unmarked attendances`() {
    mockJobs(ATTENDANCE_EXPIRE)

    whenever(jobService.incrementCount(123L)).thenReturn(true)

    service.handleEvent(123, PENTONVILLE_PRISON_CODE, today, true)

    verify(manageAttendancesService).createAttendances(today, PENTONVILLE_PRISON_CODE)

    verify(safeJobRunner).runDistributedJob(ATTENDANCE_EXPIRE, expireAttendancesService::sendEvents)
    verify(expireAttendancesService).sendEvents(nextJobCaptor.capture())
    assertThat(nextJobCaptor.firstValue.jobType).isEqualTo(ATTENDANCE_EXPIRE)

    verifyNoMoreInteractions(safeJobRunner)
    verifyNoMoreInteractions(expireAttendancesService)
  }
}
