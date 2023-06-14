package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

@Service
class ManageAttendancesService(
  private val scheduledInstanceRepository: ScheduledInstanceRepository,
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val outboundEventsService: OutboundEventsService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun attendances(operation: AttendanceOperation) {
    when (operation) {
      AttendanceOperation.CREATE -> createAttendanceRecordsForToday()
      AttendanceOperation.EXPIRE -> expireUnmarkedAttendanceRecordsOneDayAfterTheirSession()
    }
  }

  private fun createAttendanceRecordsForToday() {
    LocalDate.now().let { today ->
      log.info("Creating attendance records for date: $today")

      scheduledInstanceRepository.findAllBySessionDate(today)
        .andAttendanceRequired()
        .forEach { instance ->
          instance.forEachInFlightAllocation { allocation ->
            createAttendanceRecordIfNoPreExistingRecord(instance, allocation)
          }
        }
    }
  }

  private fun List<ScheduledInstance>.andAttendanceRequired() = filter { it.attendanceRequired() }

  private fun ScheduledInstance.forEachInFlightAllocation(f: (allocation: Allocation) -> Unit) {
    activitySchedule.allocations().filterNot { it.status(PrisonerStatus.ENDED) }.forEach { f(it) }
  }

  private fun createAttendanceRecordIfNoPreExistingRecord(instance: ScheduledInstance, allocation: Allocation) {
    if (!attendanceAlreadyExistsFor(instance, allocation)) {
      val attendance = when {
        allocation.status(PrisonerStatus.AUTO_SUSPENDED, PrisonerStatus.SUSPENDED) -> Attendance(
          scheduledInstance = instance,
          prisonerNumber = allocation.prisonerNumber,
          attendanceReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED),
          payAmount = allocation.allocationPay()?.rate,
          issuePayment = false,
          status = AttendanceStatus.COMPLETED,
          recordedTime = LocalDateTime.now(),
          recordedBy = ServiceName.SERVICE_NAME.value,
        )
        instance.cancelled -> Attendance(
          scheduledInstance = instance,
          prisonerNumber = allocation.prisonerNumber,
          payAmount = allocation.allocationPay()?.rate,
          issuePayment = true,
          status = AttendanceStatus.COMPLETED,
          attendanceReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED),
          recordedTime = LocalDateTime.now(),
          recordedBy = ServiceName.SERVICE_NAME.value,
        )
        else -> Attendance(
          scheduledInstance = instance,
          prisonerNumber = allocation.prisonerNumber,
          payAmount = allocation.allocationPay()?.rate,
        )
      }

      attendanceRepository.saveAndFlush(attendance)
    }
  }

  private fun attendanceAlreadyExistsFor(instance: ScheduledInstance, allocation: Allocation) =
    attendanceRepository.existsAttendanceByScheduledInstanceAndPrisonerNumber(instance, allocation.prisonerNumber)

  private fun expireUnmarkedAttendanceRecordsOneDayAfterTheirSession() {
    log.info("Expiring WAITING attendances from yesterday.")

    LocalDate.now().minusDays(1).let { yesterday ->
      val counter = AtomicInteger(0)
      forEachRolledOutPrison { prison ->
        attendanceRepository.findWaitingAttendancesOnDate(prison.code, yesterday)
          .forEach { waitingAttendance ->
            runCatching {
              outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_EXPIRED, waitingAttendance.attendanceId)
            }.onFailure {
              log.error("Failed to send expire event for attendance ID ${waitingAttendance.attendanceId}", it)
            }.onSuccess {
              counter.incrementAndGet()
            }
          }
      }

      log.info("${counter.get()} attendance record(s) expired.")
    }
  }

  private fun forEachRolledOutPrison(block: (RolloutPrison) -> Unit) =
    rolloutPrisonRepository.findAll().filter { it.isActivitiesRolledOut() }.forEach { block(it) }
}

enum class AttendanceOperation {
  CREATE,
  EXPIRE,
}
