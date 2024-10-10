package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstanceAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstanceCancelRequest
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
import java.util.Optional

class ScheduledInstanceServiceTest {
  private val repository: ScheduledInstanceRepository = mock()
  private val attendanceSummaryRepository: ScheduledInstanceAttendanceSummaryRepository = mock()
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository = mock()
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val service = ScheduledInstanceService(
    repository,
    attendanceReasonRepository,
    attendanceSummaryRepository,
    prisonerScheduledActivityRepository,
    outboundEventsService,
    TransactionHandler(),
    telemetryClient,
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
}
