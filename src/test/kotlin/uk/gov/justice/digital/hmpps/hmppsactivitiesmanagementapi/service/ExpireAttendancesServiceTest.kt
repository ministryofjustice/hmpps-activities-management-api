package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ATTENDANCE_EXPIRE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobEventMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsSqsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.PrisonCodeJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.Clock
import java.time.LocalDate

class ExpireAttendancesServiceTest {
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
    )
  }

  private val attendanceRepository: AttendanceRepository = mock()
  private val jobsSqsService: JobsSqsService = mock()
  private val jobService: JobService = mock()
  private val outboundEventsService: OutboundEventsService = mock()

  val service = ExpireAttendancesService(
    rolloutPrisonService,
    attendanceRepository,
    jobsSqsService,
    jobService,
    outboundEventsService,
    Clock.systemDefaultZone(),
  )

  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)

  private lateinit var activity: Activity
  private lateinit var activitySchedule: ActivitySchedule
  private lateinit var allocation: Allocation
  private lateinit var instance: ScheduledInstance
  private lateinit var attendance: Attendance

  @BeforeEach
  fun beforeEach() {
    setUpActivityWithAttendanceFor(today)
  }

  @Test
  fun `should expire attendances`() {
    service.expireUnmarkedAttendances()

    verify(attendanceRepository).findWaitingAttendancesOnDate(PENTONVILLE_PRISON_CODE, yesterday)
    verify(attendanceRepository).findWaitingAttendancesOnDate(MOORLAND_PRISON_CODE, yesterday)

    verifyNoMoreInteractions(attendanceRepository)
  }

  @Test
  fun `should send events to queue for each prison`() {
    service.sendEvents(Job(123, ATTENDANCE_EXPIRE))

    verify(jobService).initialiseCounts(123, 2)

    verify(jobsSqsService).sendJobEvent(JobEventMessage(123, ATTENDANCE_EXPIRE, PrisonCodeJobEvent(PENTONVILLE_PRISON_CODE)))
    verify(jobsSqsService).sendJobEvent(JobEventMessage(123, ATTENDANCE_EXPIRE, PrisonCodeJobEvent(MOORLAND_PRISON_CODE)))

    verifyNoInteractions(attendanceRepository)
  }

  @Test
  fun `handleEvent - an unmarked attendance for yesterday triggers an expired sync event today`() {
    setUpActivityWithAttendanceFor(yesterday)

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    attendanceRepository.stub {
      on {
        findWaitingAttendancesOnDate(MOORLAND_PRISON_CODE, yesterday)
      } doReturn listOf(attendance)
    }

    service.handleEvent(123, MOORLAND_PRISON_CODE)

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_EXPIRED, attendance.attendanceId)
  }

  @Test
  fun `handleEvent - an unmarked attendance two weeks ago does not trigger an expired sync event`() {
    setUpActivityWithAttendanceFor(yesterday.minusWeeks(2))

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    attendanceRepository.stub {
      on {
        findWaitingAttendancesOnDate(MOORLAND_PRISON_CODE, yesterday)
      } doReturn emptyList()
    }

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `a marked attendance from yesterday does not generate an expired event`() {
    setUpActivityWithAttendanceFor(yesterday)

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    attendance.mark(
      principalName = "me",
      attendanceReason(),
      AttendanceStatus.COMPLETED,
      newComment = null,
      newIssuePayment = null,
      newIncentiveLevelWarningIssued = null,
      newCaseNoteId = null,
      newDpsCaseNoteId = null,
      newOtherAbsenceReason = null,
    )

    attendanceRepository.stub {
      on {
        findWaitingAttendancesOnDate(MOORLAND_PRISON_CODE, yesterday)
      } doReturn emptyList()
    }

    service.handleEvent(123, MOORLAND_PRISON_CODE)

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)

    verify(attendanceRepository).findWaitingAttendancesOnDate(MOORLAND_PRISON_CODE, yesterday)

    verifyNoInteractions(outboundEventsService)
  }

  private fun setUpActivityWithAttendanceFor(activityStartDate: LocalDate) {
    activity =
      activityEntity(startDate = activityStartDate, timestamp = activityStartDate.atStartOfDay(), noSchedules = true)
        .apply {
          this.addSchedule(
            activitySchedule(
              this,
              activityScheduleId = activityId,
              activityStartDate.atStartOfDay(),
              daysOfWeek = setOf(activityStartDate.dayOfWeek),
              noExclusions = true,
            ),
          )
        }

    activitySchedule = activity.schedules().first()
    allocation = activitySchedule.allocations().first()
    instance = activitySchedule.instances().first()
    attendance = instance.attendances.first()
  }
}
