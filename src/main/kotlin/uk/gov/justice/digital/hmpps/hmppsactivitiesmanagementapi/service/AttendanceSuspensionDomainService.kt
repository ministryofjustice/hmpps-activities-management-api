package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import java.time.LocalDateTime

@Service
class AttendanceSuspensionDomainService(
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun suspendFutureAttendancesForAllocation(attendanceReasonToSet: AttendanceReasonEnum, dateTime: LocalDateTime, allocation: Allocation): List<Attendance> {
    require(attendanceReasonToSet in listOf(AttendanceReasonEnum.SUSPENDED, AttendanceReasonEnum.AUTO_SUSPENDED)) {
      "Failed to suspend future attendances for allocation with id ${allocation.allocationId}, because the provided attendance reason " +
        "$attendanceReasonToSet was not one of: ${AttendanceReasonEnum.SUSPENDED}, ${AttendanceReasonEnum.AUTO_SUSPENDED}"
    }

    val reason = attendanceReasonRepository.findByCode(attendanceReasonToSet)
    return attendanceRepository.findAttendancesOnOrAfterDateForAllocation(
      dateTime.toLocalDate(),
      allocation.activitySchedule.activityScheduleId,
      AttendanceStatus.WAITING,
      allocation.prisonerNumber,
    )
      .filter { attendance -> attendance.editable() && attendance.scheduledInstance.isFuture(dateTime) }
      .onEach { attendance -> attendance.completeWithoutPayment(reason) }
      .also {
        log.info("Suspended ($reason) ${it.size} attendances for allocation with ID ${allocation.allocationId}.")
      }
  }

  fun resetFutureSuspendedAttendancesForAllocation(
    attendanceReasonToReset: AttendanceReasonEnum,
    dateTime: LocalDateTime,
    allocation: Allocation,
  ): List<Attendance> {
    require(attendanceReasonToReset in listOf(AttendanceReasonEnum.SUSPENDED, AttendanceReasonEnum.AUTO_SUSPENDED)) {
      "Failed to suspend future attendances for allocation with id ${allocation.allocationId}, because the provided attendance reason " +
        "$attendanceReasonToReset was not one of: ${AttendanceReasonEnum.SUSPENDED}, ${AttendanceReasonEnum.AUTO_SUSPENDED}"
    }

    val now = LocalDateTime.now()
    val cancelledReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)
    val suspendedReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)

    return attendanceRepository.findAttendancesOnOrAfterDateForAllocation(
      dateTime.toLocalDate(),
      allocation.activitySchedule.activityScheduleId,
      AttendanceStatus.COMPLETED,
      allocation.prisonerNumber,
    )
      .filter { attendance -> attendance.hasReason(attendanceReasonToReset) }
      .filter { attendance -> attendance.editable() && attendance.scheduledInstance.isFuture(now) }
      .onEach { attendance ->
        if (attendance.scheduledInstance.cancelled) {
          // If the schedule instance was cancelled (and still is) after the initial suspension then we need to cancel instead of unsuspend
          attendance.cancel(cancelledReason).also { log.info("Cancelled attendance ${attendance.attendanceId}") }
        } else if (attendanceReasonToReset == AttendanceReasonEnum.AUTO_SUSPENDED && allocation.isCurrentlySuspended()) {
          // If we are resetting auto suspensions and the prisoner currently has a planned suspension, then we need to set the attendances as suspended
          attendance.completeWithoutPayment(suspendedReason).also { log.info("Suspended attendance ${attendance.attendanceId}") }
        } else {
          attendance.unsuspend().also { log.info("Unsuspended attendance ${attendance.attendanceId}") }
        }
      }
      .also {
        log.info("Reset ${it.size} suspended attendances for allocation with ID ${allocation.allocationId}.")
      }
  }
}
