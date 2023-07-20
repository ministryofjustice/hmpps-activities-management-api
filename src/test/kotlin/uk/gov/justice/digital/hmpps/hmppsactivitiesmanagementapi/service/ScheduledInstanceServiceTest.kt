package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstanceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseLoadIdToRequestHeader
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ScheduledInstanceServiceTest {
  private val repository: ScheduledInstanceRepository = mock()
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val service = ScheduledInstanceService(repository, attendanceReasonRepository, outboundEventsService)

  @Nested
  @DisplayName("getActivityScheduleInstanceById")
  inner class GetActivityScheduleInstanceById {

    @Test
    fun `scheduled instance found - success`() {
      addCaseLoadIdToRequestHeader("MDI")
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
  @DisplayName("getActivityScheduleInstancesByDateRange")
  inner class GetActivityScheduleInstancesByDateRange {
    @Test
    fun `get instances by date range - success`() {
      val prisonCode = "MDI"
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)
      val dateRange = LocalDateRange(startDate, endDate)

      whenever(repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(prisonCode, startDate, endDate))
        .thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

      val result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, null)

      assertThat(result).hasSize(1)
    }

    @Test
    fun `filtered by time slot`() {
      val prisonCode = "MDI"
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)
      val dateRange = LocalDateRange(startDate, endDate)

      whenever(repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(prisonCode, startDate, endDate))
        .thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

      var result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, TimeSlot.PM)
      assertThat(result).hasSize(1)

      result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, TimeSlot.AM)
      assertThat(result).isEmpty()

      result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, TimeSlot.ED)
      assertThat(result).isEmpty()
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
      )
      schedule.apply {
        this.allocatePrisoner(
          prisonerNumber = "A1234AB".toPrisonerNumber(),
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
      whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)).thenReturn(attendanceReason(AttendanceReasonEnum.CANCELLED))

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

      verify(outboundEventsService)
        .send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instance.scheduledInstanceId)
    }

    @Test
    fun `cannot cancel an already cancelled instance`() {
      val activity = activityEntity(timestamp = LocalDateTime.now())
      val schedule = activity.schedules().first()
      val instance = schedule.instances().first()
      instance.cancelled = true

      whenever(repository.findById(instance.scheduledInstanceId)).thenReturn(Optional.of(instance))
      whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)).thenReturn(attendanceReason(AttendanceReasonEnum.CANCELLED))

      assertThrows<IllegalArgumentException> {
        service.cancelScheduledInstance(
          instance.scheduledInstanceId,
          ScheduleInstanceCancelRequest("Staff unavailable", "USER1", "Resume tomorrow"),
        )
      }

      verify(outboundEventsService, times(0))
        .send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instance.scheduledInstanceId)
    }
  }
}
