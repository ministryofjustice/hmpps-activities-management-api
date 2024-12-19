package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceCreationData
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Exclusion
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PlannedSuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceCreationDataRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.UUID

class ManageAttendancesServiceTest {
  private val attendanceCreationDataRepository: AttendanceCreationDataRepository = mock()
  private val activityRepository: ActivityRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()
  private val scheduledInstanceRepository: ScheduledInstanceRepository = mock()
  private val attendanceRepository: AttendanceRepository = mock()
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val monitoringService: MonitoringService = mock()

  private val rolloutPrisonService = RolloutPrisonService("MDI", "MDI", "MDI")

  private val service = ManageAttendancesService(
    attendanceCreationDataRepository,
    activityRepository,
    allocationRepository,
    scheduledInstanceRepository,
    attendanceRepository,
    attendanceReasonRepository,
    prisonPayBandRepository,
    rolloutPrisonService,
    outboundEventsService,
    prisonerSearchApiClient,
    TransactionHandler(),
    monitoringService,
    Clock.systemDefaultZone(),
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
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      listOf(
        AttendanceCreationData(
          id = UUID.randomUUID(),
          scheduledInstanceId = 0L,
          sessionDate = LocalDate.now(),
          timeSlot = TimeSlot.AM,
          prisonerNumber = "A1234AA",
          paid = true,
          prisonPayBandId = 1L,
          prisonCode = MOORLAND_PRISON_CODE,
          activityId = 1L,
          prisonerStatus = PrisonerStatus.ACTIVE,
          allocationId = 1L,
          allocStart = LocalDate.now(),
          allocEnd = null,
          scheduleStart = LocalDate.now(),
          scheduleEnd = null,
          scheduleWeeks = 1,
          possibleExclusion = false,
          plannedDeallocationDate = null,
        ),
      ),
    )

    whenever(scheduledInstanceRepository.findAllById(listOf(0L))).thenReturn(listOf(instance))
    whenever(activityRepository.findAllById(listOf(1L))).thenReturn(listOf(activity))
    whenever(prisonPayBandRepository.findAllById(listOf(1L))).thenReturn(listOf(lowPayBand))

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
  fun `attendance is created and waiting to be marked for an active allocation where there is multiple pays`() {
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      listOf(
        AttendanceCreationData(
          id = UUID.randomUUID(),
          scheduledInstanceId = 0L,
          sessionDate = LocalDate.now(),
          timeSlot = TimeSlot.AM,
          prisonerNumber = "A1234AA",
          paid = true,
          prisonPayBandId = 1L,
          prisonCode = MOORLAND_PRISON_CODE,
          activityId = 1L,
          prisonerStatus = PrisonerStatus.ACTIVE,
          allocationId = 1L,
          allocStart = LocalDate.now(),
          allocEnd = null,
          scheduleStart = LocalDate.now(),
          scheduleEnd = null,
          scheduleWeeks = 1,
          possibleExclusion = false,
          plannedDeallocationDate = null,
        ),
      ),
    )

    whenever(prisonPayBandRepository.findAllById(listOf(1L))).thenReturn(listOf(lowPayBand))

    instance.activitySchedule.activity.addPay(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBand = lowPayBand,
      rate = 35,
      pieceRate = 45,
      pieceRateItems = 55,
      startDate = LocalDate.now().minusDays(5),
    )
    whenever(activityRepository.findAllById(listOf(1L))).thenReturn(listOf(activity))
    whenever(scheduledInstanceRepository.findAllById(listOf(0L))).thenReturn(listOf(instance))

    whenever(attendanceRepository.saveAllAndFlush(anyList()))
      .thenReturn(
        listOf(
          Attendance(
            attendanceId = 1L,
            scheduledInstance = instance,
            prisonerNumber = instance.activitySchedule.allocations().first().prisonerNumber,
            status = AttendanceStatus.WAITING,
            initialIssuePayment = true,
            payAmount = 35,
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
      assertThat(payAmount).isEqualTo(35)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, 1L)
  }

  @Test
  fun `should capture failures in monitoring service for any exceptions when creating attendances`() {
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      listOf(
        AttendanceCreationData(
          id = UUID.randomUUID(),
          scheduledInstanceId = 0L,
          sessionDate = LocalDate.now(),
          timeSlot = TimeSlot.AM,
          prisonerNumber = "A1234AA",
          paid = true,
          prisonPayBandId = 1L,
          prisonCode = MOORLAND_PRISON_CODE,
          activityId = 1L,
          prisonerStatus = PrisonerStatus.ACTIVE,
          allocationId = 1L,
          allocStart = LocalDate.now(),
          allocEnd = null,
          scheduleStart = LocalDate.now(),
          scheduleEnd = null,
          scheduleWeeks = 1,
          possibleExclusion = false,
          plannedDeallocationDate = null,
        ),
      ),
    )

    whenever(scheduledInstanceRepository.findAllById(listOf(0L))).thenReturn(listOf(instance))
    whenever(activityRepository.findAllById(listOf(1L))).thenReturn(listOf(activity))
    whenever(prisonPayBandRepository.findAllById(listOf(1L))).thenReturn(listOf(lowPayBand))
    whenever(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation))

    val exception = RuntimeException("Something went wrong")
    doThrow(exception).whenever(attendanceRepository).saveAllAndFlush(anyList())

    // instance.activitySchedule.activity.attendanceRequired = true

    // whenever(scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(any(), any(), any(), isNull(), anyOrNull())) doReturn listOf(instance)
    whenever(prisonerSearchApiClient.findByPrisonerNumbersMap(anyList())) doReturn listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234AA")).associateBy { it.prisonerNumber }

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(monitoringService).capture("Error occurred saving attendance records for prison code 'MDI'", exception)
  }

  @Test
  fun `attendance is not created if the allocation start date is in the future`() {
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      listOf(
        AttendanceCreationData(
          id = UUID.randomUUID(),
          scheduledInstanceId = 0L,
          sessionDate = LocalDate.now(),
          timeSlot = TimeSlot.AM,
          prisonerNumber = "A1234AA",
          paid = true,
          prisonPayBandId = 1L,
          prisonCode = MOORLAND_PRISON_CODE,
          activityId = 1L,
          prisonerStatus = PrisonerStatus.ACTIVE,
          allocationId = 1L,
          allocStart = TimeSource.tomorrow(),
          allocEnd = null,
          scheduleStart = LocalDate.now(),
          scheduleEnd = null,
          scheduleWeeks = 1,
          possibleExclusion = false,
          plannedDeallocationDate = null,
        ),
      ),
    )
    whenever(scheduledInstanceRepository.findAllById(listOf(0L))).thenReturn(listOf(instance))
    whenever(activityRepository.findAllById(listOf(1L))).thenReturn(listOf(activity))
    whenever(prisonPayBandRepository.findAllById(listOf(1L))).thenReturn(listOf(lowPayBand))

    whenever(prisonerSearchApiClient.findByPrisonerNumbersMap(listOf("A1234AA")))
      .thenReturn(listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234AA")).associateBy { it.prisonerNumber })

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository, times(0)).saveAllAndFlush(anyList())
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `attendance is not created if the allocation has ended`() {
    // prisoner ended are not returned by attendanceCreationDataRepository.findBy(..)
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      emptyList(),
    )

    // de-allocation set the prisoner status to ENDED
    allocation.deallocateNowWithReason(DeallocationReason.ENDED)
    whenever(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation))

    whenever(scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(MOORLAND_PRISON_CODE, today, today)) doReturn listOf(instance)
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(emptyList())) doReturn emptyList()

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository, times(0)).saveAllAndFlush(anyList())
    verifyNoInteractions(scheduledInstanceRepository)
    verifyNoInteractions(activityRepository)
    verifyNoInteractions(prisonPayBandRepository)
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `attendance is not created if the allocation is excluded`() {
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      listOf(
        AttendanceCreationData(
          id = UUID.randomUUID(),
          scheduledInstanceId = 0L,
          sessionDate = LocalDate.now(),
          timeSlot = TimeSlot.AM,
          prisonerNumber = "A1234AA",
          paid = true,
          prisonPayBandId = 1L,
          prisonCode = MOORLAND_PRISON_CODE,
          activityId = 1L,
          prisonerStatus = PrisonerStatus.SUSPENDED,
          allocationId = 1L,
          allocStart = LocalDate.now(),
          allocEnd = null,
          scheduleStart = LocalDate.now(),
          scheduleEnd = null,
          scheduleWeeks = 1,
          possibleExclusion = true,
          plannedDeallocationDate = null,
        ),
      ),
    )

    whenever(scheduledInstanceRepository.findAllById(listOf(0L))).thenReturn(listOf(instance))
    whenever(activityRepository.findAllById(listOf(1L))).thenReturn(listOf(activity))
    whenever(prisonPayBandRepository.findAllById(listOf(1L))).thenReturn(listOf(lowPayBand))

    val slot = activitySchedule.slots().first()
    allocation.apply {
      addExclusion(
        Exclusion.valueOf(
          allocation = this,
          weekNumber = slot.weekNumber,
          daysOfWeek = slot.getDaysOfWeek(),
          startDate = startDate,
          timeSlot = slot.timeSlot,
        ),
      )
    }
    whenever(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation))

    val attendees = instance.attendances.map { it.prisonerNumber }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(attendees))
      .thenReturn(
        attendees.map {
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = it)
        },
      )

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository, times(0)).saveAllAndFlush(anyList())
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `attendance is created and marked auto-suspended and unpaid when allocation is auto suspended`() {
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      listOf(
        AttendanceCreationData(
          id = UUID.randomUUID(),
          scheduledInstanceId = 0L,
          sessionDate = LocalDate.now(),
          timeSlot = TimeSlot.AM,
          prisonerNumber = "A1234AA",
          paid = true,
          prisonPayBandId = 1L,
          prisonCode = MOORLAND_PRISON_CODE,
          activityId = 1L,
          prisonerStatus = PrisonerStatus.AUTO_SUSPENDED,
          allocationId = 1L,
          allocStart = LocalDate.now(),
          allocEnd = null,
          scheduleStart = LocalDate.now(),
          scheduleEnd = null,
          scheduleWeeks = 1,
          possibleExclusion = false,
          plannedDeallocationDate = null,
        ),
      ),
    )

    whenever(scheduledInstanceRepository.findAllById(listOf(0L))).thenReturn(listOf(instance))
    whenever(activityRepository.findAllById(listOf(1L))).thenReturn(listOf(activity))
    whenever(prisonPayBandRepository.findAllById(listOf(1L))).thenReturn(listOf(lowPayBand))

    allocation.autoSuspend(today.atStartOfDay(), "reason")
    whenever(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation))

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
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      listOf(
        AttendanceCreationData(
          id = UUID.randomUUID(),
          scheduledInstanceId = 0L,
          sessionDate = LocalDate.now(),
          timeSlot = TimeSlot.AM,
          prisonerNumber = "A1234AA",
          paid = true,
          prisonPayBandId = 1L,
          prisonCode = MOORLAND_PRISON_CODE,
          activityId = 1L,
          prisonerStatus = PrisonerStatus.SUSPENDED,
          allocationId = 1L,
          allocStart = LocalDate.now(),
          allocEnd = null,
          scheduleStart = LocalDate.now(),
          scheduleEnd = null,
          scheduleWeeks = 1,
          possibleExclusion = false,
          plannedDeallocationDate = null,
        ),
      ),
    )

    whenever(scheduledInstanceRepository.findAllById(listOf(0L))).thenReturn(listOf(instance))
    whenever(activityRepository.findAllById(listOf(1L))).thenReturn(listOf(activity))
    whenever(prisonPayBandRepository.findAllById(listOf(1L))).thenReturn(listOf(lowPayBand))

    allocation.apply {
      addPlannedSuspension(
        PlannedSuspension(
          allocation = this,
          plannedStartDate = this.startDate,
          plannedBy = "Test",
        ),
      )
    }.activatePlannedSuspension()

    whenever(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation))
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
  fun `attendance is created and marked suspended and paid when an allocation is suspended with pay`() {
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      listOf(
        AttendanceCreationData(
          id = UUID.randomUUID(),
          scheduledInstanceId = 0L,
          sessionDate = LocalDate.now(),
          timeSlot = TimeSlot.AM,
          prisonerNumber = "A1234AA",
          paid = true,
          prisonPayBandId = 1L,
          prisonCode = MOORLAND_PRISON_CODE,
          activityId = 1L,
          prisonerStatus = PrisonerStatus.SUSPENDED_WITH_PAY,
          allocationId = 1L,
          allocStart = LocalDate.now(),
          allocEnd = null,
          scheduleStart = LocalDate.now(),
          scheduleEnd = null,
          scheduleWeeks = 1,
          possibleExclusion = false,
          plannedDeallocationDate = null,
        ),
      ),
    )

    whenever(scheduledInstanceRepository.findAllById(listOf(0L))).thenReturn(listOf(instance))
    whenever(activityRepository.findAllById(listOf(1L))).thenReturn(listOf(activity))
    whenever(prisonPayBandRepository.findAllById(listOf(1L))).thenReturn(listOf(lowPayBand))

    allocation.apply {
      addPlannedSuspension(
        PlannedSuspension(
          allocation = this,
          plannedStartDate = this.startDate,
          plannedBy = "Test",
        ),
      )
    }.activatePlannedSuspension()

    whenever(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation))
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
            initialIssuePayment = true,
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
      assertThat(issuePayment).isTrue
      assertThat(recordedBy).isEqualTo("Activities Management Service")
      assertThat(status()).isEqualTo(AttendanceStatus.COMPLETED)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, 1L)
  }

  @Test
  fun `attendance is created and marked cancelled and paid when the scheduled instance is cancelled`() {
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      listOf(
        AttendanceCreationData(
          id = UUID.randomUUID(),
          scheduledInstanceId = 0L,
          sessionDate = LocalDate.now(),
          timeSlot = TimeSlot.AM,
          prisonerNumber = "A1234AA",
          paid = true,
          prisonPayBandId = 1L,
          prisonCode = MOORLAND_PRISON_CODE,
          activityId = 1L,
          prisonerStatus = PrisonerStatus.ACTIVE,
          allocationId = 1L,
          allocStart = LocalDate.now(),
          allocEnd = null,
          scheduleStart = LocalDate.now(),
          scheduleEnd = null,
          scheduleWeeks = 1,
          possibleExclusion = false,
          plannedDeallocationDate = null,
        ),
      ),
    )

    instance.cancelSessionAndAttendances(
      reason = "Cancel test",
      by = "user",
      cancelComment = "comment",
      attendanceReason(AttendanceReasonEnum.CANCELLED),
    )
    whenever(scheduledInstanceRepository.findAllById(listOf(0L))).thenReturn(listOf(instance))
    whenever(activityRepository.findAllById(listOf(1L))).thenReturn(listOf(activity))
    whenever(prisonPayBandRepository.findAllById(listOf(1L))).thenReturn(listOf(lowPayBand))

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
      assertThat(comment).isEqualTo("Cancel test")
      assertThat(recordedBy).isEqualTo("user")
      assertThat(status()).isEqualTo(AttendanceStatus.COMPLETED)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, 1L)
  }

  @Test
  fun `attendance is created and marked cancelled and paid when the scheduled instance is cancelled and there are multiple pays`() {
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      listOf(
        AttendanceCreationData(
          id = UUID.randomUUID(),
          scheduledInstanceId = 0L,
          sessionDate = LocalDate.now(),
          timeSlot = TimeSlot.AM,
          prisonerNumber = "A1234AA",
          paid = true,
          prisonPayBandId = 1L,
          prisonCode = MOORLAND_PRISON_CODE,
          activityId = 1L,
          prisonerStatus = PrisonerStatus.ACTIVE,
          allocationId = 1L,
          allocStart = LocalDate.now(),
          allocEnd = null,
          scheduleStart = LocalDate.now(),
          scheduleEnd = null,
          scheduleWeeks = 1,
          possibleExclusion = false,
          plannedDeallocationDate = null,
        ),
      ),
    )

    instance.cancelSessionAndAttendances(
      reason = "Cancel test",
      by = "user",
      cancelComment = "comment",
      attendanceReason(AttendanceReasonEnum.CANCELLED),
    )
    whenever(scheduledInstanceRepository.findAllById(listOf(0L))).thenReturn(listOf(instance))

    activity.addPay(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBand = lowPayBand,
      rate = 35,
      pieceRate = 45,
      pieceRateItems = 55,
      startDate = LocalDate.now().minusDays(5),
    )
    whenever(activityRepository.findAllById(listOf(1L))).thenReturn(listOf(activity))
    whenever(prisonPayBandRepository.findAllById(listOf(1L))).thenReturn(listOf(lowPayBand))

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
            payAmount = 35,
          ),
        ),
      )

    service.createAttendances(today, MOORLAND_PRISON_CODE)

    verify(attendanceRepository).saveAllAndFlush(attendanceListCaptor.capture())

    with(attendanceListCaptor.firstValue.first()) {
      assertThat(attendanceReason).isEqualTo(attendanceReasons()["CANCELLED"])
      assertThat(payAmount).isEqualTo(35)
      assertThat(issuePayment).isTrue
      assertThat(comment).isEqualTo("Cancel test")
      assertThat(recordedBy).isEqualTo("user")
      assertThat(status()).isEqualTo(AttendanceStatus.COMPLETED)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, 1L)
  }

  @Test
  fun `attendance is created as suspended and unpaid when a session is cancelled and allocation is suspended`() {
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      listOf(
        AttendanceCreationData(
          id = UUID.randomUUID(),
          scheduledInstanceId = 0L,
          sessionDate = LocalDate.now(),
          timeSlot = TimeSlot.AM,
          prisonerNumber = "A1234AA",
          paid = true,
          prisonPayBandId = 1L,
          prisonCode = MOORLAND_PRISON_CODE,
          activityId = 1L,
          prisonerStatus = PrisonerStatus.SUSPENDED,
          allocationId = 1L,
          allocStart = LocalDate.now(),
          allocEnd = null,
          scheduleStart = LocalDate.now(),
          scheduleEnd = null,
          scheduleWeeks = 1,
          possibleExclusion = false,
          plannedDeallocationDate = null,
        ),
      ),
    )

    instance.cancelSessionAndAttendances(
      reason = "Cancel test",
      by = "user",
      cancelComment = "comment",
      attendanceReason(AttendanceReasonEnum.CANCELLED),
    )
    whenever(scheduledInstanceRepository.findAllById(listOf(0L))).thenReturn(listOf(instance))

    whenever(activityRepository.findAllById(listOf(1L))).thenReturn(listOf(activity))
    whenever(prisonPayBandRepository.findAllById(listOf(1L))).thenReturn(listOf(lowPayBand))

    allocation.apply {
      addPlannedSuspension(
        PlannedSuspension(
          allocation = this,
          plannedStartDate = this.startDate,
          plannedBy = "Test",
        ),
      )
    }.activatePlannedSuspension()

    whenever(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation))

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
  fun `attendance is created when attendance is not required on the activity`() {
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      listOf(
        AttendanceCreationData(
          id = UUID.randomUUID(),
          scheduledInstanceId = 0L,
          sessionDate = LocalDate.now(),
          timeSlot = TimeSlot.AM,
          prisonerNumber = "A1234AA",
          paid = true,
          prisonPayBandId = 1L,
          prisonCode = MOORLAND_PRISON_CODE,
          activityId = 1L,
          prisonerStatus = PrisonerStatus.ACTIVE,
          allocationId = 1L,
          allocStart = LocalDate.now(),
          allocEnd = null,
          scheduleStart = LocalDate.now(),
          scheduleEnd = null,
          scheduleWeeks = 1,
          possibleExclusion = false,
          plannedDeallocationDate = null,
        ),
      ),
    )

    whenever(scheduledInstanceRepository.findAllById(listOf(0L))).thenReturn(listOf(instance))

    activity.attendanceRequired = false
    whenever(activityRepository.findAllById(listOf(1L))).thenReturn(listOf(activity))

    whenever(prisonPayBandRepository.findAllById(listOf(1L))).thenReturn(listOf(lowPayBand))
    whenever(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation))

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
  fun `attendance is not created if a pre-existing attendance exists for this session and allocation`() {
    // not attendances are not returned by attendanceCreationDataRepository.findBy(..) if they already exist for a session and prison number
    whenever(attendanceCreationDataRepository.findBy(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(
      emptyList(),
    )

    // de-allocation set the prisoner status to ENDED
    allocation.deallocateNowWithReason(DeallocationReason.ENDED)
    whenever(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation))

    whenever(scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(MOORLAND_PRISON_CODE, today, today)) doReturn listOf(instance)
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(emptyList())) doReturn emptyList()

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
    verifyNoInteractions(scheduledInstanceRepository)
    verifyNoInteractions(activityRepository)
    verifyNoInteractions(prisonPayBandRepository)
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

  @Nested
  @DisplayName("Create attendances for today")
  inner class CreateAttendances {
    private lateinit var allocation: Allocation

    @BeforeEach
    fun init() {
      allocation = allocation()
    }

    @Test
    fun `should do nothing if no schedule instance id was provided`() {
      assertThat(service.createAnyAttendancesForToday(null, allocation)).isEmpty()
    }

    @Test
    fun `should throw an exception if the provided schedule id does not match the allocation schedule id`() {
      whenever(scheduledInstanceRepository.findById(123)).thenReturn(Optional.of(instance.copy(123)))

      assertThatThrownBy {
        service.createAnyAttendancesForToday(123, allocation)
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Allocation does not belong to same activity schedule as selected instance")
    }

    @Test
    fun `should create an attendance record`() {
      whenever(scheduledInstanceRepository.findById(1)).thenReturn(Optional.of(allocation.activitySchedule.instances()[0]))

      val attendances = service.createAnyAttendancesForToday(1, allocation)

      assertThat(attendances).hasSize(1)
      assertThat(attendances.first().scheduledInstance).isEqualTo(allocation.activitySchedule.instances().first())
    }

    @Test
    fun `should not create an attendance record if session date is not today`() {
      allocation.activitySchedule.removeInstances(allocation.activitySchedule.instances())
      allocation.activitySchedule.addInstance(TimeSource.tomorrow(), allocation.activitySchedule.slots().first())

      whenever(scheduledInstanceRepository.findById(1)).thenReturn(Optional.of(allocation.activitySchedule.instances()[0]))

      val attendances = service.createAnyAttendancesForToday(1, allocation)

      assertThat(attendances).isEmpty()
    }

    @Test
    fun `should only create an attendance records if session time is on or after the selected scheduled instance start time`() {
      val firstSlot = allocation.activitySchedule.slots().first()

      allocation.activitySchedule.addSlot(firstSlot.weekNumber, firstSlot.startTime.plusHours(1) to firstSlot.endTime.plusHours(1), firstSlot.getDaysOfWeek(), firstSlot.timeSlot)
      allocation.activitySchedule.addSlot(firstSlot.weekNumber, firstSlot.startTime.plusHours(2) to firstSlot.endTime.plusHours(2), firstSlot.getDaysOfWeek(), firstSlot.timeSlot)

      allocation.activitySchedule.addInstance(TimeSource.today(), allocation.activitySchedule.slots()[1])
      allocation.activitySchedule.addInstance(TimeSource.today(), allocation.activitySchedule.slots()[2])

      whenever(scheduledInstanceRepository.findById(1)).thenReturn(Optional.of(allocation.activitySchedule.instances()[1]))
      whenever(scheduledInstanceRepository.findById(2)).thenReturn(Optional.of(allocation.activitySchedule.instances()[2]))

      val attendances = service.createAnyAttendancesForToday(1, allocation)

      // Should not contain first instance as it is before the second instance that is selected
      assertThat(attendances).hasSize(2)
      assertThat(attendances[0].scheduledInstance).isEqualTo(allocation.activitySchedule.instances()[1])
      assertThat(attendances[1].scheduledInstance).isEqualTo(allocation.activitySchedule.instances()[2])
    }

    @Test
    fun `should only create an attendance records if prisoner is able to attend`() {
      allocation.deallocateNowWithReason(DeallocationReason.ENDED)

      whenever(scheduledInstanceRepository.findById(1)).thenReturn(Optional.of(allocation.activitySchedule.instances()[0]))

      val attendances = service.createAnyAttendancesForToday(1, allocation)

      assertThat(attendances).isEmpty()
    }
  }

  @Nested
  @DisplayName("Delete attendances for today")
  inner class DeleteAttendances {
    private lateinit var allocation: Allocation
    private lateinit var nextInstanceToday: ScheduledInstance
    private lateinit var lastInstanceToday: ScheduledInstance
    private lateinit var firstInstanceTomorrow: ScheduledInstance
    private lateinit var joeBloggsTodayAMAttendance: Attendance
    private lateinit var joeBloggsTodayPMAttendance: Attendance
    private lateinit var joeBloggsTodayEDAttendance: Attendance
    private lateinit var frankSmithTodayEDAttendance: Attendance
    private lateinit var joeBloggsTomorrowPMAttendance: Attendance

    @BeforeEach
    fun init() {
      allocation = allocation()
      val pmStart = LocalTime.now().plusMinutes(10)
      val edStart = pmStart.plusMinutes(30)
      val pm = allocation.activitySchedule.addSlot(1, pmStart to pmStart.plusMinutes(30), setOf(DayOfWeek.MONDAY), TimeSlot.PM)
      val ed = allocation.activitySchedule.addSlot(1, edStart to edStart.plusMinutes(30), setOf(DayOfWeek.MONDAY), TimeSlot.ED)

      nextInstanceToday = allocation.activitySchedule.addInstance(sessionDate = LocalDate.now(), slot = pm)
      lastInstanceToday = allocation.activitySchedule.addInstance(sessionDate = LocalDate.now(), slot = ed)
      firstInstanceTomorrow = allocation.activitySchedule.addInstance(sessionDate = LocalDate.now().plusDays(1), slot = pm)

      joeBloggsTodayAMAttendance = allocation.activitySchedule.instances().first().attendances.first()
      joeBloggsTodayPMAttendance = joeBloggsTodayAMAttendance.copy(scheduledInstance = nextInstanceToday)
      joeBloggsTodayEDAttendance = joeBloggsTodayAMAttendance.copy(scheduledInstance = lastInstanceToday)
      frankSmithTodayEDAttendance = joeBloggsTodayAMAttendance.copy(scheduledInstance = lastInstanceToday, prisonerNumber = "AAAAAA")
      joeBloggsTomorrowPMAttendance = joeBloggsTodayAMAttendance.copy(scheduledInstance = firstInstanceTomorrow)

      nextInstanceToday.attendances.add(joeBloggsTodayPMAttendance)
      lastInstanceToday.attendances.add(joeBloggsTodayEDAttendance)
      lastInstanceToday.attendances.add(frankSmithTodayEDAttendance)
      firstInstanceTomorrow.attendances.add(joeBloggsTomorrowPMAttendance)
    }

    @Test
    fun `should do nothing if no schedule instance id was provided`() {
      assertThat(service.deleteAnyAttendancesForToday(null, allocation)).isEmpty()
    }

    @Test
    fun `should not delete attendances if schedule instance id is null`() {
      whenever(scheduledInstanceRepository.findById(123)).doReturn(Optional.of(nextInstanceToday))

      val deletedAttendances = service.deleteAnyAttendancesForToday(123L, allocation)

      assertThat(deletedAttendances).containsOnly(joeBloggsTodayPMAttendance, joeBloggsTodayEDAttendance)

      assertThat(allocation.activitySchedule.instances().first().attendances).containsOnly(joeBloggsTodayAMAttendance)
      assertThat(nextInstanceToday.attendances).isEmpty()
      assertThat(lastInstanceToday.attendances).containsOnly(frankSmithTodayEDAttendance)
      assertThat(firstInstanceTomorrow.attendances).containsOnly(joeBloggsTomorrowPMAttendance)
    }

    @Test
    fun `should throw an exception if the provided schedule id does not match the allocation schedule id`() {
      whenever(scheduledInstanceRepository.findById(123)).thenReturn(Optional.of(instance.copy(123)))

      assertThatThrownBy {
        service.deleteAnyAttendancesForToday(123, allocation)
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Allocation does not belong to same activity schedule as selected instance")

      assertThat(allocation.activitySchedule.instances().first().attendances).containsOnly(joeBloggsTodayAMAttendance)
      assertThat(nextInstanceToday.attendances).containsOnly(joeBloggsTodayPMAttendance)
      assertThat(lastInstanceToday.attendances).containsOnly(joeBloggsTodayEDAttendance, frankSmithTodayEDAttendance)
      assertThat(firstInstanceTomorrow.attendances).containsOnly(joeBloggsTomorrowPMAttendance)
    }
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
