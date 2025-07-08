package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstanceAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityFromDbInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstanceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstancesCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstancesUncancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduledInstancedUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ScheduledAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceAttendanceSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryPropertiesMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class ScheduledInstanceServiceTest {
  private val repository: ScheduledInstanceRepository = mock()
  private val attendanceSummaryRepository: ScheduledInstanceAttendanceSummaryRepository = mock()
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository = mock()
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val featureSwitches: FeatureSwitches = mock { on { isEnabled(any<Feature>(), any()) } doReturn false }
  private val service = ScheduledInstanceService(
    repository,
    attendanceReasonRepository,
    attendanceSummaryRepository,
    prisonerScheduledActivityRepository,
    outboundEventsService,
    TransactionHandler(),
    telemetryClient,
    featureSwitches,
  )

  @Nested
  @DisplayName("getActivityScheduleInstanceById")
  inner class GetActivityScheduleInstanceById {

    @Test
    fun `scheduled instance found - success`() {
      addCaseloadIdToRequestHeader("MDI")
      whenever(repository.findById(1))
        .thenReturn(Optional.of(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

      assertThat(service.getActivityScheduleInstanceById(1))
        .isInstanceOf(ActivityScheduleInstance::class.java)
    }

    @Test
    fun `scheduled instance not found`() {
      whenever(repository.findById(1)).thenReturn(Optional.empty())
      assertThatThrownBy { service.getActivityScheduleInstanceById(-1) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Scheduled Instance -1 not found")
    }
  }

  @Nested
  @DisplayName("getActivityScheduleInstancesByIds")
  inner class GetActivityScheduleInstancesByIds {

    @Test
    fun `scheduled instances found - success`() {
      addCaseloadIdToRequestHeader("MDI")
      whenever(repository.findByIds(listOf(1, 2, 3)))
        .thenReturn(
          listOf(
            ScheduledInstanceFixture.instance(id = 1, locationId = 11),
            ScheduledInstanceFixture.instance(id = 2, locationId = 22),
            ScheduledInstanceFixture.instance(id = 3, locationId = 33),
          ),
        )

      assertThat(service.getActivityScheduleInstancesByIds(listOf(1, 2, 3)))
        .extracting<Long> { it.id }.containsOnly(1, 2, 3)
    }
  }

  @Nested
  @DisplayName("getActivityScheduleInstancesByDateRange")
  inner class GetActivityScheduleInstancesByDateRange {
    val prisonCode = "MDI"
    val startDate = LocalDate.of(2022, 10, 1)
    val endDate = LocalDate.of(2022, 11, 5)
    val dateRange = LocalDateRange(startDate, endDate)

    @Test
    fun `get instances by date range - success`() {
      whenever(repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(prisonCode, startDate, endDate, null))
        .thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

      val result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, null, null)

      assertThat(result).hasSize(1)
    }

    @Test
    fun `filtered by time slot`() {
      val scheduledInstance = ScheduledInstanceFixture.instance(id = 1, locationId = 22)
      whenever(repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(prisonCode, startDate, endDate, null, TimeSlot.PM))
        .thenReturn(listOf(scheduledInstance))

      var result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, TimeSlot.PM, null)
      assertThat(result).hasSize(1)

      result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, TimeSlot.AM, null)
      assertThat(result).isEmpty()

      result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, TimeSlot.ED, null)
      assertThat(result).isEmpty()
    }

    @Test
    fun `filtered for cancelled instances`() {
      whenever(repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(prisonCode, startDate, endDate, true))
        .thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

      var result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, null, true)
      assertThat(result).hasSize(1)

      result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, null, false)
      assertThat(result).isEmpty()
    }

    @Test
    fun `filtered for non-cancelled instances`() {
      whenever(repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(prisonCode, startDate, endDate, false))
        .thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

      var result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, null, false)
      assertThat(result).hasSize(1)

      result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, null, true)
      assertThat(result).isEmpty()
    }
  }

  @Nested
  @DisplayName("getActivityScheduleInstancesForPrisonerByDateRange")
  inner class GetActivityScheduleInstancesForPrisonerByDateRange {
    val prisonCode = "MDI"
    val prisonerNumber = "A1234AA"
    val startDate: LocalDate = LocalDate.of(2022, 10, 1)
    val endDate: LocalDate = LocalDate.of(2022, 11, 5)
    val results = listOf(activityFromDbInstance())

    @Test
    fun `get prisoner activities`() {
      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
          prisonCode = prisonCode,
          prisonerNumber = prisonerNumber,
          startDate = startDate,
          endDate = endDate,
          timeSlot = null,
        ),
      )
        .thenReturn(results)

      val result = service.getActivityScheduleInstancesForPrisonerByDateRange(prisonCode = prisonCode, prisonerNumber = prisonerNumber, startDate = startDate, endDate = endDate, slot = null)
      assertThat(result).hasSize(1)
    }

    @Test
    fun `filtered by time slot`() {
      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
          prisonCode = prisonCode,
          prisonerNumber = prisonerNumber,
          startDate = startDate,
          endDate = endDate,
          timeSlot = TimeSlot.PM,
        ),
      )
        .thenReturn(results)

      var result = service.getActivityScheduleInstancesForPrisonerByDateRange(prisonCode = prisonCode, prisonerNumber = prisonerNumber, startDate = startDate, endDate = endDate, slot = TimeSlot.PM)
      assertThat(result).hasSize(1)

      result = service.getActivityScheduleInstancesForPrisonerByDateRange(prisonCode = prisonCode, prisonerNumber = prisonerNumber, startDate = startDate, endDate = endDate, slot = TimeSlot.AM)
      assertThat(result).isEmpty()

      result = service.getActivityScheduleInstancesForPrisonerByDateRange(prisonCode = prisonCode, prisonerNumber = prisonerNumber, startDate = startDate, endDate = endDate, slot = TimeSlot.ED)
      assertThat(result).isEmpty()
    }

    @Test
    fun `returns exception when there is more than 3 months between start and end date`() {
      assertThatThrownBy {
        service.getActivityScheduleInstancesForPrisonerByDateRange(
          prisonCode = prisonCode,
          prisonerNumber = prisonerNumber,
          startDate = startDate,
          endDate = startDate.plusMonths(3).plusDays(1),
          slot = null,
        )
      }
        .isInstanceOf(ValidationException::class.java)
        .hasMessage("Date range cannot exceed 3 months")
    }
  }

  @Nested
  @DisplayName("getAttendeesForScheduledInstance")
  inner class GetAttendeesForScheduledInstance {
    @Test
    fun `get attendees by instance - success`() {
      addCaseloadIdToRequestHeader("MDI")
      whenever(repository.findById(1)).thenReturn(Optional.of(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))
      whenever(prisonerScheduledActivityRepository.getAllByScheduledInstanceId(1)).thenReturn(
        listOf(
          PrisonerScheduledActivity(
            scheduledInstanceId = 1,
            allocationId = 2,
            prisonCode = "MDI",
            sessionDate = LocalDate.now(),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            prisonerNumber = "ABC123",
            bookingId = 100001,
            inCell = false,
            onWing = false,
            offWing = true,
            activityCategory = "SAA_OUT_OF_WORK",
            activityId = 1,
            timeSlot = TimeSlot.AM,
            paidActivity = true,
            issuePayment = true,
            attendanceStatus = AttendanceStatus.COMPLETED,
            attendanceReasonCode = AttendanceReasonEnum.ATTENDED,
            possibleAdvanceAttendance = false,
          ),
        ),
      )

      val result = service.getAttendeesForScheduledInstance(1)

      assertThat(result).hasSize(1)
      assertThat(result).isEqualTo(
        listOf(
          ScheduledAttendee(
            scheduledInstanceId = 1,
            allocationId = 2,
            prisonerNumber = "ABC123",
            bookingId = 100001,
            suspended = false,
          ),
        ),
      )
    }

    @Test
    fun `get attendees by instance including without attendance - success`() {
      val activityWithAttendance = PrisonerScheduledActivity(
        scheduledInstanceId = 1,
        allocationId = 2,
        prisonCode = "MDI",
        sessionDate = LocalDate.now(),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
        prisonerNumber = "ABC123",
        bookingId = 100001,
        inCell = false,
        onWing = false,
        offWing = true,
        activityCategory = "SAA_OUT_OF_WORK",
        activityId = 1,
        timeSlot = TimeSlot.AM,
        paidActivity = true,
        issuePayment = true,
        attendanceStatus = AttendanceStatus.COMPLETED,
        attendanceReasonCode = AttendanceReasonEnum.ATTENDED,
        possibleAdvanceAttendance = false,
      )

      val activityWithoutAttendance = activityWithAttendance.copy(attendanceStatus = null)

      addCaseloadIdToRequestHeader("MDI")
      whenever(repository.findById(1)).thenReturn(Optional.of(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))
      whenever(prisonerScheduledActivityRepository.getAllByScheduledInstanceId(1)).thenReturn(
        listOf(activityWithAttendance, activityWithoutAttendance),
      )

      val result = service.getAttendeesForScheduledInstance(1)

      assertThat(result).hasSize(1)
      assertThat(result).isEqualTo(
        listOf(
          ScheduledAttendee(
            scheduledInstanceId = 1,
            allocationId = 2,
            prisonerNumber = "ABC123",
            bookingId = 100001,
            suspended = false,
          ),
        ),
      )
    }

    @Test
    fun `get attendees by instance - not found`() {
      whenever(repository.findById(1)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> { service.getAttendeesForScheduledInstance(1) }
      assertThat(exception.message).isEqualTo("Scheduled Instance 1 not found")
    }
  }

  @Nested
  @DisplayName("uncancelScheduledInstance")
  inner class UncancelScheduledInstance {
    @Test
    fun `un-cancels a scheduled instance - success`() {
      val instance = activityEntity(timestamp = LocalDateTime.now()).schedules().first().instances().first()
      instance.apply {
        cancelled = true
        attendances.first().cancel(attendanceReason(AttendanceReasonEnum.CANCELLED))
      }

      whenever(repository.findById(instance.scheduledInstanceId)).thenReturn(Optional.of(instance))
      whenever(repository.saveAndFlush(instance)).thenReturn(instance)

      service.uncancelScheduledInstance(instance.scheduledInstanceId)

      assertThat(instance.cancelled).isFalse
      verify(repository).saveAndFlush(instance)
      verify(outboundEventsService).send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instance.scheduledInstanceId)
      verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, instance.attendances.first().attendanceId)
    }

    @Test
    fun `throws exception when scheduled instance ID is not found`() {
      whenever(repository.findById(1)).thenReturn(Optional.empty())
      val exception = assertThrows<EntityNotFoundException> {
        service.uncancelScheduledInstance(1)
      }
      assertThat(exception.message).isEqualTo("Scheduled Instance 1 not found")
    }
  }

  @Nested
  @DisplayName("cancelScheduledInstance")
  inner class CancelScheduledInstance {
    @Test
    @Deprecated("Remove when toggle FEATURE_CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED is removed")
    fun `cancels a scheduled instance and its attendance - success - old`() {
      val activity = activityEntity(timestamp = LocalDateTime.now())
      val schedule = activity.schedules().first()
      val instance = schedule.instances().first()
      instance.cancelled = false

      activity.addPay(
        incentiveNomisCode = "STD",
        incentiveLevel = "Standard",
        payBand = prisonPayBandsLowMediumHigh()[1],
        rate = 50,
        pieceRate = 60,
        pieceRateItems = 70,
        startDate = null,
      )
      schedule.apply {
        this.allocatePrisoner(
          prisonerNumber = "A1234AB".toPrisonerNumber(),
          startDate = LocalDate.now().plusDays(1),
          bookingId = 10002,
          payBand = prisonPayBandsLowMediumHigh()[1],
          allocatedBy = "Mr Blogs",
        )
      }
      instance.apply {
        this.attendances.add(
          Attendance(
            attendanceId = 2,
            scheduledInstance = this,
            prisonerNumber = "A1234AB",
          ),
        )
      }

      whenever(repository.findById(instance.scheduledInstanceId)).thenReturn(Optional.of(instance))
      whenever(repository.saveAndFlush(instance)).thenReturn(instance)
      whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)).thenReturn(
        attendanceReason(
          AttendanceReasonEnum.CANCELLED,
        ),
      )

      service.cancelScheduledInstance(
        instance.scheduledInstanceId,
        ScheduleInstanceCancelRequest("Staff unavailable", "USER1", "Resume tomorrow"),
      )

      assertThat(instance.cancelled).isTrue
      assertThat(instance.cancelledTime).isNotNull
      assertThat(instance.cancelledBy).isEqualTo("USER1")
      assertThat(instance.comment).isEqualTo("Resume tomorrow")

      instance.attendances.forEach {
        assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
        assertThat(it.attendanceReason?.code).isEqualTo(AttendanceReasonEnum.CANCELLED)
        assertThat(it.comment).isEqualTo("Staff unavailable")
        assertThat(it.recordedBy).isEqualTo("USER1")
        assertThat(it.recordedTime).isNotNull
      }

      verify(telemetryClient).trackEvent(TelemetryEvent.RECORD_ATTENDANCE.value, instance.attendances.first().toTelemetryPropertiesMap())
      verify(outboundEventsService)
        .send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instance.scheduledInstanceId)
      verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, instance.attendances.first().attendanceId)
    }

    @Test
    fun `cancels a scheduled instance and its attendance - success`() {
      val activity = activityEntity(timestamp = LocalDateTime.now())
      val schedule = activity.schedules().first()
      val instance = schedule.instances().first()
      instance.cancelled = false

      activity.addPay(
        incentiveNomisCode = "STD",
        incentiveLevel = "Standard",
        payBand = prisonPayBandsLowMediumHigh()[1],
        rate = 50,
        pieceRate = 60,
        pieceRateItems = 70,
        startDate = null,
      )
      schedule.apply {
        this.allocatePrisoner(
          prisonerNumber = "A1234AB".toPrisonerNumber(),
          startDate = LocalDate.now().plusDays(1),
          bookingId = 10002,
          payBand = prisonPayBandsLowMediumHigh()[1],
          allocatedBy = "Mr Blogs",
        )
      }
      instance.apply {
        this.attendances.add(
          Attendance(
            attendanceId = 2,
            scheduledInstance = this,
            prisonerNumber = "A1234AB",
          ),
        )
      }

      whenever(repository.findById(instance.scheduledInstanceId)).thenReturn(Optional.of(instance))
      whenever(repository.saveAndFlush(instance)).thenReturn(instance)
      whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)).thenReturn(
        attendanceReason(
          AttendanceReasonEnum.CANCELLED,
        ),
      )

      featureSwitches.stub { on { isEnabled(Feature.CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED) } doReturn true }

      service.cancelScheduledInstance(
        instance.scheduledInstanceId,
        ScheduleInstanceCancelRequest("Staff unavailable", "USER1", "Resume tomorrow"),
      )

      assertThat(instance.cancelled).isTrue
      assertThat(instance.cancelledTime).isNotNull
      assertThat(instance.cancelledBy).isEqualTo("USER1")
      assertThat(instance.comment).isEqualTo("Resume tomorrow")

      instance.attendances.forEach {
        assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
        assertThat(it.attendanceReason?.code).isEqualTo(AttendanceReasonEnum.CANCELLED)
        assertThat(it.comment).isEqualTo("Staff unavailable")
        assertThat(it.recordedBy).isEqualTo("USER1")
        assertThat(it.recordedTime).isNotNull
      }

      verify(telemetryClient).trackEvent(TelemetryEvent.RECORD_ATTENDANCE.value, instance.attendances.first().toTelemetryPropertiesMap())
      verify(outboundEventsService)
        .send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instance.scheduledInstanceId)
      verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, instance.attendances.first().attendanceId)
    }

    @Test
    fun `cannot cancel an already cancelled instance`() {
      val activity = activityEntity(timestamp = LocalDateTime.now())
      val schedule = activity.schedules().first()
      val instance = schedule.instances().first()
      instance.cancelled = true

      whenever(repository.findById(instance.scheduledInstanceId)).thenReturn(Optional.of(instance))
      whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)).thenReturn(
        attendanceReason(
          AttendanceReasonEnum.CANCELLED,
        ),
      )

      assertThrows<IllegalArgumentException> {
        service.cancelScheduledInstance(
          instance.scheduledInstanceId,
          ScheduleInstanceCancelRequest("Staff unavailable", "USER1", "Resume tomorrow"),
        )
      }

      verify(outboundEventsService, times(0))
        .send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instance.scheduledInstanceId)
    }

    @Test
    fun `can get scheduled instance attendance summary`() {
      val attendanceSummary = ScheduledInstanceAttendanceSummary(
        scheduledInstanceId = 1,
        activityId = 1,
        activityScheduleId = 2,
        prisonCode = "MDI",
        summary = "English 1",
        activityCategoryId = 4,
        sessionDate = LocalDate.now(),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(12, 0),
        inCell = true,
        onWing = false,
        offWing = false,
        cancelled = false,
        allocations = 3,
        attendees = 3,
        notRecorded = 1,
        attended = 1,
        absences = 1,
        paid = 1,
        attendanceRequired = true,
        timeSlot = TimeSlot.AM,
      )

      whenever(attendanceSummaryRepository.findByPrisonAndDate("MDI", LocalDate.now()))
        .thenReturn(listOf(attendanceSummary))

      addCaseloadIdToRequestHeader("MDI")

      val result = service.attendanceSummary("MDI", LocalDate.now())

      assertThat(result).isEqualTo(listOf(attendanceSummary.toModel()))
    }
  }

  @Nested
  @DisplayName("cancelScheduledInstances")
  inner class CancelScheduledInstances {
    val today = LocalDate.now()
    var instance1: ScheduledInstance? = null
    var instance2: ScheduledInstance? = null

    @BeforeEach
    fun setUp() {
      val activity1 = activityEntity(
        activityId = 1L,
        timestamp = LocalDateTime.now(),
        noSchedules = true,
        description = "Maths Level 1",
      )
      val activity2 = activityEntity(
        activityId = 2L,
        timestamp = LocalDateTime.now(),
        noSchedules = true,
        description = "English Level 2",
        noPayBands = true,
        paid = false,
      )

      val schedule1 = activity1.addSchedule(
        activitySchedule(
          activityScheduleId = 1L,
          activity = activity1,
          description = activity1.description!!,
          paid = true,
          timeSlot = TimeSlot.AM,
          noInstances = true,
        ),
      )
      val schedule2 = activity2.addSchedule(
        activitySchedule(
          activityScheduleId = 2L,
          activity = activity2,
          description = activity2.description!!,
          paid = false,
          timeSlot = TimeSlot.AM,
          noInstances = true,
        ),
      )

      instance1 = schedule1.addInstance(sessionDate = today, slot = schedule1.slots().first())
      instance2 = schedule2.addInstance(sessionDate = today, slot = schedule2.slots().first())

      instance1!!.cancelled = false
      instance2!!.cancelled = false

      activity1.addPay(
        incentiveNomisCode = "STD",
        incentiveLevel = "Standard",
        payBand = prisonPayBandsLowMediumHigh()[1],
        rate = 50,
        pieceRate = 60,
        pieceRateItems = 70,
        startDate = null,
      )
      schedule1.apply {
        this.allocatePrisoner(
          prisonerNumber = "A1234AB".toPrisonerNumber(),
          startDate = today.plusDays(1),
          bookingId = 10002,
          payBand = prisonPayBandsLowMediumHigh()[1],
          allocatedBy = "Mr Blogs",
        )
      }
      instance1.apply {
        this!!.attendances.add(
          Attendance(
            attendanceId = 2,
            scheduledInstance = this,
            prisonerNumber = "A1234AB",
          ),
        )
      }

      schedule2.apply {
        this.allocatePrisoner(
          prisonerNumber = "B1111BB".toPrisonerNumber(),
          startDate = today.plusDays(1),
          bookingId = 10003,
          payBand = null,
          allocatedBy = "John Smith",
        )
      }
      instance2.apply {
        this!!.attendances.add(
          Attendance(
            attendanceId = 3,
            scheduledInstance = this,
            prisonerNumber = "B1111BB",
          ),
        )
      }

      whenever(
        repository.findByIds(
          listOf(
            instance1!!.scheduledInstanceId,
            instance2!!.scheduledInstanceId,
          ),
        ),
      ).thenReturn(listOf(instance1!!, instance2!!))
      whenever(repository.saveAllAndFlush(listOf(instance1, instance2))).thenReturn(listOf(instance1, instance2))
      whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)).thenReturn(
        attendanceReason(
          AttendanceReasonEnum.CANCELLED,
        ),
      )
    }

    @Test
    @Deprecated("Remove when toggle FEATURE_CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED is removed")
    fun `cancels the scheduled instances and their attendances - success - old`() {
      service.cancelScheduledInstances(
        ScheduleInstancesCancelRequest(
          scheduleInstanceIds = listOf(instance1!!.scheduledInstanceId, instance2!!.scheduledInstanceId),
          reason = "Staff unavailable",
          username = "USER1",
          comment = "Resume tomorrow",
          issuePayment = true,
        ),
      )

      with(instance1!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledTime).isNotNull
        assertThat(cancelledBy).isEqualTo("USER1")
        assertThat(comment).isEqualTo("Resume tomorrow")

        attendances.forEach {
          assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
          assertThat(it.attendanceReason?.code).isEqualTo(AttendanceReasonEnum.CANCELLED)
          assertThat(it.comment).isEqualTo("Staff unavailable")
          assertThat(it.recordedBy).isEqualTo("USER1")
          assertThat(it.recordedTime).isNotNull
          assertThat(it.issuePayment).isTrue
        }

        verify(telemetryClient).trackEvent(
          TelemetryEvent.RECORD_ATTENDANCE.value,
          attendances.first().toTelemetryPropertiesMap(),
        )
        verify(outboundEventsService).send(
          OutboundEvent.PRISONER_ATTENDANCE_AMENDED,
          attendances.first().attendanceId,
        )
      }

      with(instance2!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledTime).isNotNull
        assertThat(cancelledBy).isEqualTo("USER1")
        assertThat(comment).isEqualTo("Resume tomorrow")

        attendances.forEach {
          assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
          assertThat(it.attendanceReason?.code).isEqualTo(AttendanceReasonEnum.CANCELLED)
          assertThat(it.comment).isEqualTo("Staff unavailable")
          assertThat(it.recordedBy).isEqualTo("USER1")
          assertThat(it.recordedTime).isNotNull
          assertThat(it.issuePayment).isFalse
        }

        verify(telemetryClient).trackEvent(
          TelemetryEvent.RECORD_ATTENDANCE.value,
          attendances.first().toTelemetryPropertiesMap(),
        )
        verify(outboundEventsService).send(
          OutboundEvent.PRISONER_ATTENDANCE_AMENDED,
          attendances.first().attendanceId,
        )
      }

      verify(outboundEventsService, times(2))
        .send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instance1!!.scheduledInstanceId)
    }

    @Test
    fun `cancels the scheduled instances and their attendances - success`() {
      featureSwitches.stub { on { isEnabled(Feature.CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED) } doReturn true }

      service.cancelScheduledInstances(
        ScheduleInstancesCancelRequest(
          scheduleInstanceIds = listOf(instance1!!.scheduledInstanceId, instance2!!.scheduledInstanceId),
          reason = "Staff unavailable",
          username = "USER1",
          comment = "Resume tomorrow",
          issuePayment = true,
        ),
      )

      with(instance1!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledTime).isNotNull
        assertThat(cancelledBy).isEqualTo("USER1")
        assertThat(comment).isEqualTo("Resume tomorrow")

        attendances.forEach {
          assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
          assertThat(it.attendanceReason?.code).isEqualTo(AttendanceReasonEnum.CANCELLED)
          assertThat(it.comment).isEqualTo("Staff unavailable")
          assertThat(it.recordedBy).isEqualTo("USER1")
          assertThat(it.recordedTime).isNotNull
          assertThat(it.issuePayment).isTrue
        }

        verify(telemetryClient).trackEvent(
          TelemetryEvent.RECORD_ATTENDANCE.value,
          attendances.first().toTelemetryPropertiesMap(),
        )
        verify(outboundEventsService).send(
          OutboundEvent.PRISONER_ATTENDANCE_AMENDED,
          attendances.first().attendanceId,
        )
      }

      with(instance2!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledTime).isNotNull
        assertThat(cancelledBy).isEqualTo("USER1")
        assertThat(comment).isEqualTo("Resume tomorrow")

        attendances.forEach {
          assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
          assertThat(it.attendanceReason?.code).isEqualTo(AttendanceReasonEnum.CANCELLED)
          assertThat(it.comment).isEqualTo("Staff unavailable")
          assertThat(it.recordedBy).isEqualTo("USER1")
          assertThat(it.recordedTime).isNotNull
          assertThat(it.issuePayment).isFalse
        }

        verify(telemetryClient).trackEvent(
          TelemetryEvent.RECORD_ATTENDANCE.value,
          attendances.first().toTelemetryPropertiesMap(),
        )
        verify(outboundEventsService).send(
          OutboundEvent.PRISONER_ATTENDANCE_AMENDED,
          attendances.first().attendanceId,
        )
      }

      verify(outboundEventsService, times(2))
        .send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instance1!!.scheduledInstanceId)
    }

    @Test
    @Deprecated("Remove when toggle FEATURE_CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED is removed")
    fun `will not cancel any instances if one instance is already cancelled - failed - old`() {
      instance1!!.cancelled = true

      assertThatThrownBy {
        service.cancelScheduledInstances(
          ScheduleInstancesCancelRequest(
            scheduleInstanceIds = listOf(instance1!!.scheduledInstanceId, instance2!!.scheduledInstanceId),
            reason = "Staff unavailable",
            username = "USER1",
            comment = "Resume tomorrow",
            issuePayment = true,
          ),
        )
      }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("${instance1!!.activitySchedule.description} (${instance1!!.timeSlot}) has already been cancelled")

      verifyNoInteractions(telemetryClient)
      verifyNoInteractions(outboundEventsService)
      verify(repository, never()).saveAllAndFlush(anyList())
    }

    @Test
    fun `will not cancel any instances if one instance is already cancelled - failed`() {
      instance1!!.cancelled = true

      assertThatThrownBy {
        featureSwitches.stub { on { isEnabled(Feature.CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED) } doReturn true }

        service.cancelScheduledInstances(
          ScheduleInstancesCancelRequest(
            scheduleInstanceIds = listOf(instance1!!.scheduledInstanceId, instance2!!.scheduledInstanceId),
            reason = "Staff unavailable",
            username = "USER1",
            comment = "Resume tomorrow",
            issuePayment = true,
          ),
        )
      }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("${instance1!!.activitySchedule.description} (${instance1!!.timeSlot}) has already been cancelled")

      verifyNoInteractions(telemetryClient)
      verifyNoInteractions(outboundEventsService)
      verify(repository, never()).saveAllAndFlush(anyList())
    }
  }

  @Nested
  @DisplayName("uncancelScheduledInstances")
  inner class UncancelScheduledInstances {
    @Test
    fun `uncancels the scheduled instances and their attendances - success`() {
      val activity1 = activityEntity(activityId = 1L, noSchedules = true)
      val activity2 = activityEntity(activityId = 2L, noSchedules = true)

      val schedule1 = activity1.addSchedule(activitySchedule(activityScheduleId = 1L, activity = activity1, noInstances = true))
      val schedule2 = activity2.addSchedule(activitySchedule(activityScheduleId = 2L, activity = activity2, noInstances = true))

      val today = LocalDate.now()
      val instance1 = schedule1.addInstance(sessionDate = today, slot = schedule1.slots().first()).copy(scheduledInstanceId = 1, cancelled = true)
      val instance2 = schedule2.addInstance(sessionDate = today, slot = schedule2.slots().first()).copy(scheduledInstanceId = 2, cancelled = true)

      instance1.attendances.add(Attendance(attendanceId = 111, scheduledInstance = instance1, prisonerNumber = "A1111AA", status = AttendanceStatus.COMPLETED, recordedTime = LocalDateTime.now()))
      instance2.attendances.add(Attendance(attendanceId = 222, scheduledInstance = instance1, prisonerNumber = "B1111BB", status = AttendanceStatus.COMPLETED, recordedTime = LocalDateTime.now()))
      instance2.attendances.add(Attendance(attendanceId = 333, scheduledInstance = instance1, prisonerNumber = "C1111CC", status = AttendanceStatus.COMPLETED, recordedTime = LocalDateTime.now()))

      whenever(repository.findByIds(listOf(instance1.scheduledInstanceId, instance2.scheduledInstanceId))).thenReturn(listOf(instance1, instance2))

      service.uncancelScheduledInstances(ScheduleInstancesUncancelRequest(scheduleInstanceIds = listOf(instance1.scheduledInstanceId, instance2.scheduledInstanceId)))

      assertThat(instance1.cancelled).isFalse

      verify(repository).saveAllAndFlush(listOf(instance1, instance2))

      verify(outboundEventsService).send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instance1.scheduledInstanceId)
      verify(outboundEventsService).send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instance2.scheduledInstanceId)

      verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 111)
      verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 222)
      verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 333)
    }
  }

  @Nested
  @DisplayName("updateScheduledInstance")
  inner class UpdateScheduledInstance {
    @BeforeEach
    fun setUp() {
      addCaseloadIdToRequestHeader("MDI")
    }

    @Test
    fun `updates the scheduled instance`() {
      val instance: ScheduledInstance = activityEntity(timestamp = LocalDateTime.now()).schedules().first().instances().first()

      instance.apply {
        cancelled = true
        attendances.first().cancel(attendanceReason(AttendanceReasonEnum.CANCELLED))
      }

      whenever(repository.findById(instance.scheduledInstanceId)).thenReturn(Optional.of(instance))
      whenever(repository.saveAndFlush(instance)).thenReturn(instance)

      val request = ScheduledInstancedUpdateRequest("Cancelled reason", "Comment", true)
      service.updateScheduledInstance(instance.scheduledInstanceId, request, "USER1")

      with(instance) {
        assertThat(cancelled).isTrue()
        assertThat(cancelledReason).isEqualTo("Cancelled reason")
      }

      verify(repository).saveAndFlush(instance)
      verify(outboundEventsService).send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instance.scheduledInstanceId)
      verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, instance.attendances.first().attendanceId)
    }

    @Test
    fun `throws exception when scheduled instance ID is not found`() {
      whenever(repository.findById(1)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        val request = ScheduledInstancedUpdateRequest("Cancelled reason", "Comment", true)
        service.updateScheduledInstance(1, request, "USER1")
      }

      assertThat(exception.message).isEqualTo("Scheduled Instance 1 not found")
    }
  }
}
