package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDateTime

@Service
@Transactional
class AttendanceSuspensionService(
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun suspendFutureAttendancesForAllocation(dateTime: LocalDateTime, allocation: Allocation) {
    transactionHandler.newSpringTransaction {
      val reason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)
      attendanceRepository.findAttendancesOnOrAfterDateForAllocation(
        dateTime.toLocalDate(),
        allocation.activitySchedule.activityScheduleId,
        AttendanceStatus.WAITING,
        allocation.prisonerNumber,
      )
        .filter { attendance -> attendance.editable() && attendance.scheduledInstance.isFuture(dateTime) }
        .onEach { attendance -> attendance.completeWithoutPayment(reason) }
        .also {
          attendanceRepository.saveAllAndFlush(it)
          log.info("Suspended ${it.size} attendances for allocation with ID ${allocation.allocationId}.")
        }
    }.let(::sendAttendanceAmendedEvents)
  }

  fun resetFutureSuspendedAttendancesForAllocation(
    dateTime: LocalDateTime,
    allocation: Allocation,
  ) {
    transactionHandler.newSpringTransaction {
      val now = LocalDateTime.now()
      val cancelledReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)

      attendanceRepository.findAttendancesOnOrAfterDateForAllocation(
        dateTime.toLocalDate(),
        allocation.activitySchedule.activityScheduleId,
        AttendanceStatus.COMPLETED,
        allocation.prisonerNumber,
      )
        .filter { attendance -> attendance.hasReason(AttendanceReasonEnum.SUSPENDED) }
        .filter { attendance -> attendance.editable() && attendance.scheduledInstance.isFuture(now) }
        .onEach { attendance ->
          if (attendance.scheduledInstance.cancelled) {
            // If the schedule instance was cancelled (and still is) after the initial suspension then we need to cancel instead of unsuspend
            attendance.cancel(cancelledReason).also { log.info("Cancelled attendance ${attendance.attendanceId}") }
          } else {
            attendance.unsuspend().also { log.info("Unsuspended attendance ${attendance.attendanceId}") }
          }
        }
        .also {
          attendanceRepository.saveAllAndFlush(it)
          log.info("Reset ${it.size} suspended attendances for allocation with ID ${allocation.allocationId}.")
        }
    }.let(::sendAttendanceAmendedEvents)
  }

  private fun sendAttendanceAmendedEvents(attendances: List<Attendance>) =
    attendances.forEach {
      outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, it.attendanceId)
    }.also { log.info("Sending attendance amended events.") }
}
