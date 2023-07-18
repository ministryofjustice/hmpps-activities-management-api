package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
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
    on { code } doReturn moorlandPrisonCode
    on { isActivitiesRolledOut() } doReturn true
  }
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock {
    on { findAll() } doReturn listOf(rolloutPrison)
  }

  private val service = ManageAttendancesService(
    scheduledInstanceRepository,
    attendanceRepository,
    attendanceReasonRepository,
    rolloutPrisonRepository,
    outboundEventsService,
    prisonerSearchApiClient,
  )

  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val attendanceCaptor = argumentCaptor<Attendance>()

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
  fun `attendance record is created when no pre-existing attendance record, attendance is required and allocation active`() {
    instance.activitySchedule.activity.attendanceRequired = true

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    val attendees = instance.attendances.map { it.prisonerNumber }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(attendees)).thenReturn(
      Mono.just(
        attendees.map { PrisonerSearchPrisonerFixture.instance(prisonerNumber = it) },
      ),
    )

    service.attendances(AttendanceOperation.CREATE)

    verify(attendanceRepository).saveAndFlush(
      Attendance(
        scheduledInstance = instance,
        prisonerNumber = instance.activitySchedule.allocations().first().prisonerNumber,
        status = AttendanceStatus.WAITING,
        payAmount = 30,
      ),
    )
  }

  @Test
  fun `attendance record is not created when allocation is pending`() {
    instance.activitySchedule.activity.attendanceRequired = true

    allocation.prisonerStatus = PrisonerStatus.PENDING

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(emptyList())).thenReturn(Mono.just(emptyList()))

    service.attendances(AttendanceOperation.CREATE)

    verifyNoInteractions(attendanceRepository)
  }

  @Test
  fun `attendance record is not created when allocation has ended`() {
    instance.activitySchedule.activity.attendanceRequired = true

    allocation.deallocateNowWithReason(DeallocationReason.ENDED)

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(emptyList())).thenReturn(Mono.just(emptyList()))

    service.attendances(AttendanceOperation.CREATE)

    verifyNoInteractions(attendanceRepository)
  }

  @Test
  fun `attendance record is created and marked as not attended when allocation is auto suspended`() {
    instance.activitySchedule.activity.attendanceRequired = true
    allocation.autoSuspend(today.atStartOfDay(), "reason")

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)).thenReturn(attendanceReasons()["SUSPENDED"])
    val attendees = instance.attendances.map { it.prisonerNumber }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(attendees))
      .thenReturn(
        Mono.just(
          attendees.map {
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = it)
          },
        ),
      )

    service.attendances(AttendanceOperation.CREATE)

    verify(attendanceRepository).saveAndFlush(attendanceCaptor.capture())
    with(attendanceCaptor.firstValue) {
      assertThat(attendanceReason).isEqualTo(attendanceReasons()["SUSPENDED"])
      assertThat(recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(issuePayment).isFalse
      assertThat(recordedBy).isEqualTo("Activities Management Service")
      assertThat(status()).isEqualTo(AttendanceStatus.COMPLETED)
    }
  }

  @Test
  fun `attendance record is created and marked as not attended when allocation is suspended`() {
    instance.activitySchedule.activity.attendanceRequired = true
    allocation.userSuspend(today.atStartOfDay(), "reason", "user")

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)).thenReturn(attendanceReasons()["SUSPENDED"])
    val attendees = instance.attendances.map { it.prisonerNumber }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(attendees))
      .thenReturn(
        Mono.just(
          attendees.map {
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = it)
          },
        ),
      )

    service.attendances(AttendanceOperation.CREATE)

    verify(attendanceRepository).saveAndFlush(attendanceCaptor.capture())
    with(attendanceCaptor.firstValue) {
      assertThat(attendanceReason).isEqualTo(attendanceReasons()["SUSPENDED"])
      assertThat(recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(issuePayment).isFalse
      assertThat(recordedBy).isEqualTo("Activities Management Service")
      assertThat(status()).isEqualTo(AttendanceStatus.COMPLETED)
    }
  }

  @Test
  fun `attendance record is created, paid and marked as not required when scheduled instance is cancelled`() {
    instance.activitySchedule.activity.attendanceRequired = true
    instance.cancelSessionAndAttendances("Cancel test", "user", "comment", attendanceReason(AttendanceReasonEnum.CANCELLED))

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)).thenReturn(attendanceReasons()["CANCELLED"])

    val attendees = instance.attendances.map { it.prisonerNumber }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(attendees))
      .thenReturn(
        Mono.just(
          attendees.map {
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = it)
          },
        ),
      )

    service.attendances(AttendanceOperation.CREATE)

    verify(attendanceRepository).saveAndFlush(attendanceCaptor.capture())
    with(attendanceCaptor.firstValue) {
      assertThat(attendanceReason).isEqualTo(attendanceReasons()["CANCELLED"])
      assertThat(recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(payAmount).isEqualTo(30)
      assertThat(issuePayment).isTrue
      assertThat(recordedBy).isEqualTo("Activities Management Service")
      assertThat(status()).isEqualTo(AttendanceStatus.COMPLETED)
    }
  }

  @Test
  fun `attendance record is created and marked as not attended when scheduled instance is cancelled but allocation suspended`() {
    instance.activitySchedule.activity.attendanceRequired = true
    allocation.userSuspend(today.atStartOfDay(), "reason", "user")
    instance.cancelSessionAndAttendances("Cancel test", "user", "comment", attendanceReason(AttendanceReasonEnum.CANCELLED))

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)).thenReturn(attendanceReasons()["SUSPENDED"])
    val attendees = instance.attendances.map { it.prisonerNumber }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(attendees))
      .thenReturn(
        Mono.just(
          attendees.map {
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = it)
          },
        ),
      )

    service.attendances(AttendanceOperation.CREATE)

    verify(attendanceRepository).saveAndFlush(attendanceCaptor.capture())
    with(attendanceCaptor.firstValue) {
      assertThat(attendanceReason).isEqualTo(attendanceReasons()["SUSPENDED"])
      assertThat(recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(issuePayment).isFalse
      assertThat(recordedBy).isEqualTo("Activities Management Service")
      assertThat(status()).isEqualTo(AttendanceStatus.COMPLETED)
    }
  }

  @Test
  fun `attendance record is not created when no pre-existing attendance record and attendance is not required`() {
    instance.activitySchedule.activity.attendanceRequired = false

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    val attendees = instance.attendances.map { it.prisonerNumber }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(attendees))
      .thenReturn(
        Mono.just(
          attendees.map {
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = it)
          },
        ),
      )

    service.attendances(AttendanceOperation.CREATE)

    verify(attendanceRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `attendance record is not created if pre-existing attendance record`() {
    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    whenever(
      attendanceRepository.existsAttendanceByScheduledInstanceAndPrisonerNumber(
        instance,
        allocation.prisonerNumber,
      ),
    ).thenReturn(true)
    val attendees = instance.attendances.map { it.prisonerNumber }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(attendees))
      .thenReturn(
        Mono.just(
          attendees.map {
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = it)
          },
        ),
      )

    service.attendances(AttendanceOperation.CREATE)

    verify(attendanceRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `an unmarked attendance record for yesterday triggers an expired sync event`() {
    setUpActivityWithAttendanceFor(yesterday)

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    attendanceRepository.stub {
      on {
        findWaitingAttendancesOnDate(moorlandPrisonCode, yesterday)
      } doReturn listOf(attendance)
    }

    service.attendances(AttendanceOperation.EXPIRE)

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_EXPIRED, attendance.attendanceId)
  }

  @Test
  fun `an unmarked attendance two weeks ago does not trigger an expired sync event`() {
    setUpActivityWithAttendanceFor(yesterday.minusWeeks(2))

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    attendanceRepository.stub {
      on {
        findWaitingAttendancesOnDate(moorlandPrisonCode, yesterday)
      } doReturn emptyList()
    }

    service.attendances(AttendanceOperation.EXPIRE)

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `a marked attendance from yesterday does not generate an expired event`() {
    setUpActivityWithAttendanceFor(yesterday)

    attendance.mark("me", attendanceReason(), AttendanceStatus.COMPLETED, null, null, null, null, null)
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)

    attendanceRepository.stub {
      on {
        findWaitingAttendancesOnDate(moorlandPrisonCode, yesterday)
      } doReturn listOf(attendance)
    }

    service.attendances(AttendanceOperation.EXPIRE)

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_EXPIRED, attendance.attendanceId)
  }

  private fun setUpActivityWithAttendanceFor(activityStartDate: LocalDate) {
    activity = activityEntity(startDate = activityStartDate, timestamp = activityStartDate.atStartOfDay())
    activitySchedule = activity.schedules().first()
    allocation = activitySchedule.allocations().first()
    instance = activitySchedule.instances().first()
    attendance = instance.attendances.first()
  }
}
