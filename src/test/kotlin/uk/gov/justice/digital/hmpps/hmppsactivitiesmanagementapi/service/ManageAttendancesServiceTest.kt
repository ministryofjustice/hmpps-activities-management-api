package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Exclusion
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PlannedSuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ManageAttendancesServiceTest {
  private val scheduledInstanceRepository: ScheduledInstanceRepository = mock()
  private val attendanceRepository: AttendanceRepository = mock()
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient = mock()

  private val rolloutPrison: RolloutPrison = mock {
    on { code } doReturn MOORLAND_PRISON_CODE
    on { isActivitiesRolledOut() } doReturn true
  }

  private val rolloutPrisonRepository: RolloutPrisonRepository = mock {
    on { findAll() } doReturn listOf(rolloutPrison)
    on { findByCode(MOORLAND_PRISON_CODE) } doReturn rolloutPrison
  }

  private val service = ManageAttendancesService(
    scheduledInstanceRepository,
    attendanceRepository,
    attendanceReasonRepository,
    rolloutPrisonRepository,
    outboundEventsService,
    prisonerSearchApiClient,
    TransactionHandler(),
  )

  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val attendanceListCaptor = argumentCaptor<List<Attendance>>()

  private lateinit var activity: Activity
  private lateinit var activitySchedule: ActivitySchedule
  private lateinit var allocation: Allocation
  private lateinit var instance: ScheduledInstance
  private lateinit var attendance: Attendance

  @BeforeEach
  fun beforeEach() {
    setUpActivityWithAttendanceFor(today)
    reset(scheduledInstanceRepository, outboundEventsService, attendanceReasonRepository, attendanceRepository)
  }

  @Test
  fun `cannot create attendances in the future`() {
    assertThatThrownBy {
      service.createAttendances(TimeSource.tomorrow(), PENTONVILLE_PRISON_CODE)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot create attendance for prison '$PENTONVILLE_PRISON_CODE', date is in the future '${TimeSource.tomorrow()}'")
  }

  @Test
  fun `cannot create attendances prison not rolled out`() {
    assertThatThrownBy {
      service.createAttendances(TimeSource.today(), PENTONVILLE_PRISON_CODE)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot create attendance for prison '$PENTONVILLE_PRISON_CODE', not rolled out")
  }

  @Test
  fun `attendance is created and waiting to be marked for an active allocation`() {
    instance.activitySchedule.activity.attendanceRequired = true

    whenever(scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(MOORLAND_PRISON_CODE, today, today)) doReturn listOf(instance)
    whenever(attendanceRepository.saveAllAndFlush(anyList()))
      .thenReturn(
        listOf(
          Attendance(
            attendanceId = 1L,
            scheduledInstance = instance,
            prisonerNumber = instance.activitySchedule.allocations().first().prisonerNumber,
            status = AttendanceStatus.WAITING,
            initialIssuePayment = true,
            payAmount = 30,
          ),
        ),
      )

    whenever(prisonerSearchApiClient.findByPrisonerNumbersMap(listOf("A1234AA")))
      .thenReturn(listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234AA")).associateBy { it.prisonerNumber })

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository).saveAllAndFlush(attendanceListCaptor.capture())

    with(attendanceListCaptor.firstValue.first()) {
      assertThat(attendanceId).isEqualTo(0L) // Not set when called
      assertThat(status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(prisonerNumber).isEqualTo(instance.activitySchedule.allocations().first().prisonerNumber)
      assertThat(payAmount).isEqualTo(30)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, 1L)
  }

  @Test
  fun `attendance is not created if the allocation start date is in the future`() {
    instance.activitySchedule.activity.attendanceRequired = true

    allocation.startDate = TimeSource.tomorrow()

    whenever(scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(MOORLAND_PRISON_CODE, today, today)) doReturn listOf(instance)
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(emptyList())) doReturn emptyList()

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository, times(0)).saveAllAndFlush(anyList())
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `attendance is not created if the allocation has ended`() {
    instance.activitySchedule.activity.attendanceRequired = true

    allocation.deallocateNowWithReason(DeallocationReason.ENDED)

    whenever(scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(MOORLAND_PRISON_CODE, today, today)) doReturn listOf(instance)
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(emptyList())) doReturn emptyList()

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository, times(0)).saveAllAndFlush(anyList())
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `attendance is not created if the allocation is excluded`() {
    instance.activitySchedule.activity.attendanceRequired = true

    val slot = activitySchedule.slots().first()
    allocation.apply {
      addExclusion(Exclusion.valueOf(this, slot.startTime to slot.endTime, slot.weekNumber, slot.getDaysOfWeek(), startDate))
    }

    whenever(scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(MOORLAND_PRISON_CODE, today, today)) doReturn listOf(instance)
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(emptyList())) doReturn emptyList()

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository, times(0)).saveAllAndFlush(anyList())
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `attendance is created and marked suspended and unpaid when allocation is auto suspended`() {
    instance.activitySchedule.activity.attendanceRequired = true
    allocation.autoSuspend(today.atStartOfDay(), "reason")

    whenever(scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(MOORLAND_PRISON_CODE, today, today)) doReturn listOf(instance)
    whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.AUTO_SUSPENDED)).thenReturn(attendanceReasons()["AUTO_SUSPENDED"])

    val attendees = instance.attendances.map { it.prisonerNumber }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(attendees))
      .thenReturn(
        attendees.map {
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = it)
        },
      )

    whenever(attendanceRepository.saveAllAndFlush(anyList()))
      .thenReturn(
        listOf(
          Attendance(
            attendanceId = 1L,
            scheduledInstance = instance,
            prisonerNumber = instance.activitySchedule.allocations().first().prisonerNumber,
            status = AttendanceStatus.COMPLETED,
            attendanceReason = attendanceReasons()["AUTO_SUSPENDED"],
            initialIssuePayment = false,
            recordedTime = LocalDateTime.now(),
            recordedBy = "Activities Management Service",
          ),
        ),
      )

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository).saveAllAndFlush(attendanceListCaptor.capture())

    with(attendanceListCaptor.firstValue.first()) {
      assertThat(attendanceReason).isEqualTo(attendanceReasons()["AUTO_SUSPENDED"])
      assertThat(recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(issuePayment).isFalse
      assertThat(recordedBy).isEqualTo("Activities Management Service")
      assertThat(status()).isEqualTo(AttendanceStatus.COMPLETED)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, 1L)
  }

  @Test
  fun `attendance is created and marked suspended and unpaid when an allocation is suspended`() {
    instance.activitySchedule.activity.attendanceRequired = true

    allocation.apply {
      addPlannedSuspension(
        PlannedSuspension(
          allocation = this,
          plannedStartDate = this.startDate,
          plannedBy = "Test",
        ),
      )
    }.activatePlannedSuspension()

    whenever(scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(MOORLAND_PRISON_CODE, today, today)) doReturn listOf(instance)
    whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)).thenReturn(attendanceReasons()["SUSPENDED"])

    val attendees = instance.attendances.map { it.prisonerNumber }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(attendees))
      .thenReturn(
        attendees.map {
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = it)
        },
      )

    whenever(attendanceRepository.saveAllAndFlush(anyList()))
      .thenReturn(
        listOf(
          Attendance(
            attendanceId = 1L,
            scheduledInstance = instance,
            prisonerNumber = instance.activitySchedule.allocations().first().prisonerNumber,
            status = AttendanceStatus.COMPLETED,
            attendanceReason = attendanceReasons()["SUSPENDED"],
            initialIssuePayment = false,
            recordedTime = LocalDateTime.now(),
            recordedBy = "Activities Management Service",
          ),
        ),
      )

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository).saveAllAndFlush(attendanceListCaptor.capture())

    with(attendanceListCaptor.firstValue.first()) {
      assertThat(attendanceReason).isEqualTo(attendanceReasons()["SUSPENDED"])
      assertThat(recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(issuePayment).isFalse
      assertThat(recordedBy).isEqualTo("Activities Management Service")
      assertThat(status()).isEqualTo(AttendanceStatus.COMPLETED)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, 1L)
  }

  @Test
  fun `attendance is created and marked cancelled and paid when the scheduled instance is cancelled`() {
    instance.activitySchedule.activity.attendanceRequired = true

    instance.cancelSessionAndAttendances(
      reason = "Cancel test",
      by = "user",
      cancelComment = "comment",
      attendanceReason(AttendanceReasonEnum.CANCELLED),
    )

    whenever(scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(MOORLAND_PRISON_CODE, today, today)) doReturn listOf(instance)
    whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)).thenReturn(attendanceReasons()["CANCELLED"])

    whenever(prisonerSearchApiClient.findByPrisonerNumbersMap(listOf("A1234AA")))
      .thenReturn(listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234AA")).associateBy { it.prisonerNumber })

    whenever(attendanceRepository.saveAllAndFlush(anyList()))
      .thenReturn(
        listOf(
          Attendance(
            attendanceId = 1L,
            scheduledInstance = instance,
            prisonerNumber = instance.activitySchedule.allocations().first().prisonerNumber,
            status = AttendanceStatus.COMPLETED,
            attendanceReason = attendanceReasons()["CANCELLED"],
            initialIssuePayment = true,
            payAmount = 30,
          ),
        ),
      )

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository).saveAllAndFlush(attendanceListCaptor.capture())

    with(attendanceListCaptor.firstValue.first()) {
      assertThat(attendanceReason).isEqualTo(attendanceReasons()["CANCELLED"])
      assertThat(payAmount).isEqualTo(30)
      assertThat(issuePayment).isTrue
      assertThat(status()).isEqualTo(AttendanceStatus.COMPLETED)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, 1L)
  }

  @Test
  fun `attendance is created as suspended and unpaid when a session is cancelled and allocation is suspended`() {
    instance.activitySchedule.activity.attendanceRequired = true

    allocation.apply {
      addPlannedSuspension(
        PlannedSuspension(
          allocation = this,
          plannedStartDate = this.startDate,
          plannedBy = "Test",
        ),
      )
    }.activatePlannedSuspension()

    instance.cancelSessionAndAttendances(
      reason = "Cancel test",
      by = "user",
      cancelComment = "comment",
      attendanceReason(AttendanceReasonEnum.CANCELLED),
    )

    whenever(scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(MOORLAND_PRISON_CODE, today, today)) doReturn listOf(instance)
    whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)).thenReturn(attendanceReasons()["SUSPENDED"])

    val attendees = instance.attendances.map { it.prisonerNumber }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(attendees))
      .thenReturn(
        attendees.map {
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = it)
        },
      )

    whenever(attendanceRepository.saveAllAndFlush(anyList()))
      .thenReturn(
        listOf(
          Attendance(
            attendanceId = 1L,
            scheduledInstance = instance,
            prisonerNumber = instance.activitySchedule.allocations().first().prisonerNumber,
            status = AttendanceStatus.COMPLETED,
            attendanceReason = attendanceReasons()["SUSPENDED"],
            initialIssuePayment = false,
          ),
        ),
      )

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository).saveAllAndFlush(attendanceListCaptor.capture())

    with(attendanceListCaptor.firstValue.first()) {
      assertThat(attendanceReason).isEqualTo(attendanceReasons()["SUSPENDED"])
      assertThat(recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(issuePayment).isFalse
      assertThat(recordedBy).isEqualTo("Activities Management Service")
      assertThat(status()).isEqualTo(AttendanceStatus.COMPLETED)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, 1L)
  }

  @Test
  fun `attendance is not created when attendance is not required on the activity`() {
    instance.activitySchedule.activity.attendanceRequired = false

    whenever(scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(MOORLAND_PRISON_CODE, today, today)) doReturn listOf(instance)

    val attendees = instance.attendances.map { it.prisonerNumber }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(attendees))
      .thenReturn(
        attendees.map {
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = it)
        },
      )

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository, never()).saveAllAndFlush(anyList())

    verifyNoInteractions(attendanceRepository)
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `attendance is not created if a pre-existing attendance exists for this session and allocation`() {
    whenever(scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(MOORLAND_PRISON_CODE, today, today)) doReturn listOf(instance)

    whenever(
      attendanceRepository.existsAttendanceByScheduledInstanceAndPrisonerNumber(
        instance,
        allocation.prisonerNumber,
      ),
    ).thenReturn(true)

    val attendees = instance.attendances.map { it.prisonerNumber }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(attendees))
      .thenReturn(
        attendees.map {
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = it)
        },
      )

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository, never()).saveAllAndFlush(anyList())
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `an unmarked attendance for yesterday triggers an expired sync event today`() {
    setUpActivityWithAttendanceFor(yesterday)

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    attendanceRepository.stub {
      on {
        findWaitingAttendancesOnDate(MOORLAND_PRISON_CODE, yesterday)
      } doReturn listOf(attendance)
    }

    service.expireUnmarkedAttendanceRecordsOneDayAfterTheirSession()

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_EXPIRED, attendance.attendanceId)
  }

  @Test
  fun `an unmarked attendance two weeks ago does not trigger an expired sync event`() {
    setUpActivityWithAttendanceFor(yesterday.minusWeeks(2))
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    attendanceRepository.stub {
      on {
        findWaitingAttendancesOnDate(MOORLAND_PRISON_CODE, yesterday)
      } doReturn emptyList()
    }

    service.expireUnmarkedAttendanceRecordsOneDayAfterTheirSession()

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
      newOtherAbsenceReason = null,
    )

    attendanceRepository.stub {
      on {
        findWaitingAttendancesOnDate(MOORLAND_PRISON_CODE, yesterday)
      } doReturn emptyList()
    }

    service.expireUnmarkedAttendanceRecordsOneDayAfterTheirSession()

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    verifyNoInteractions(outboundEventsService)
  }

  private fun setUpActivityWithAttendanceFor(activityStartDate: LocalDate) {
    activity = activityEntity(startDate = activityStartDate, timestamp = activityStartDate.atStartOfDay(), noSchedules = true)
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
