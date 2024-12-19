package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AttendanceReasonRepository
import java.time.LocalDateTime

@Service
class AttendanceSuspensionDomainService(
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun suspendFutureAttendancesForAllocation(dateTime: LocalDateTime, allocation: Allocation, issuePayment: Boolean): List<Attendance> {
    val reason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)
    return completeFutureAttendances(reason, dateTime, allocation, issuePayment).also {
      log.info("Suspended ($reason) ${it.size} attendances for allocation with ID ${allocation.allocationId}.")
    }
  }

  fun autoSuspendFutureAttendancesForAllocation(dateTime: LocalDateTime, allocation: Allocation): List<Attendance> {
    val reason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.AUTO_SUSPENDED)
    return completeFutureAttendances(reason, dateTime, allocation).also {
      log.info("Auto-suspended ($reason) ${it.size} attendances for allocation with ID ${allocation.allocationId}.")
    }
  }

  fun resetSuspendedFutureAttendancesForAllocation(
    dateTime: LocalDateTime,
    allocation: Allocation,
  ): List<Attendance> {
    return resetFutureAttendances(AttendanceReasonEnum.SUSPENDED, dateTime, allocation).also {
      log.info("Reset ${it.size} suspended attendances for allocation with ID ${allocation.allocationId}.")
    }
  }

  fun resetAutoSuspendedFutureAttendancesForAllocation(
    dateTime: LocalDateTime,
    allocation: Allocation,
  ): List<Attendance> {
    val suspendedReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)

    return resetFutureAttendances(AttendanceReasonEnum.AUTO_SUSPENDED, dateTime, allocation)
      .onEach { attendance ->
        if (allocation.isCurrentlySuspended()) {
          // If we are resetting auto suspensions and the prisoner currently has a planned suspension,
          // then we need to set the attendances as suspended
          val paidSuspension = allocation.isCurrentlyPaidSuspension()
          attendance.complete(reason = suspendedReason, issuePayment = paidSuspension).also { log.info("Suspended attendance ${attendance.attendanceId}") }
        }
      }
      .also {
        log.info("Reset ${it.size} suspended attendances for allocation with ID ${allocation.allocationId}.")
      }
  }

  private fun completeFutureAttendances(reason: AttendanceReason, dateTime: LocalDateTime, allocation: Allocation, issuePayment: Boolean = false): List<Attendance> {
    return attendanceRepository.findAttendancesOnOrAfterDateForAllocation(
      dateTime.toLocalDate(),
      allocation.activitySchedule.activityScheduleId,
      AttendanceStatus.WAITING,
      allocation.prisonerNumber,
    )
      .filter { attendance -> attendance.editable() && attendance.scheduledInstance.isFuture(dateTime) }
      .onEach { attendance -> attendance.complete(reason = reason, issuePayment = issuePayment) }
  }

  private fun resetFutureAttendances(reasonToReset: AttendanceReasonEnum, dateTime: LocalDateTime, allocation: Allocation): List<Attendance> {
    val now = LocalDateTime.now()
    val cancelledReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)

    return attendanceRepository.findAttendancesOnOrAfterDateForAllocation(
      dateTime.toLocalDate(),
      allocation.activitySchedule.activityScheduleId,
      AttendanceStatus.COMPLETED,
      allocation.prisonerNumber,
    )
      .filter { attendance -> attendance.hasReason(reasonToReset) }
      .filter { attendance -> attendance.editable() && attendance.scheduledInstance.isFuture(now) }
      .onEach { attendance ->
        if (attendance.scheduledInstance.cancelled) {
          // If the schedule instance was cancelled (and still is) after the initial suspension then we need to cancel instead of unsuspend
          attendance.cancel(cancelledReason).also { log.info("Cancelled attendance ${attendance.attendanceId}") }
        } else {
          attendance.unsuspend()
        }
      }
  }
}
