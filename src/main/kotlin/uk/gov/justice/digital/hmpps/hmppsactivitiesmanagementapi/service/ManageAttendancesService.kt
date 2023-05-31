package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

@Service
class ManageAttendancesService(
  private val scheduledInstanceRepository: ScheduledInstanceRepository,
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun attendances(operation: AttendanceOperation) {
    when (operation) {
      AttendanceOperation.CREATE -> createAttendanceRecordsForToday()
      AttendanceOperation.LOCK -> lockAttendanceRecordsStartingFromYesterdayCountingBack()
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
          payAmount = instance.rateFor(allocation.payBand),
          issuePayment = false,
          status = AttendanceStatus.COMPLETED,
          recordedTime = LocalDateTime.now(),
          recordedBy = ServiceName.SERVICE_NAME.value,
        )
        instance.cancelled -> Attendance(
          scheduledInstance = instance,
          prisonerNumber = allocation.prisonerNumber,
          payAmount = instance.rateFor(allocation.payBand),
          issuePayment = true,
          status = AttendanceStatus.COMPLETED,
          attendanceReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED),
          recordedTime = LocalDateTime.now(),
          recordedBy = ServiceName.SERVICE_NAME.value,
        )
        else -> Attendance(
          scheduledInstance = instance,
          prisonerNumber = allocation.prisonerNumber,
          payAmount = instance.rateFor(allocation.payBand),
        )
      }

      attendanceRepository.saveAndFlush(attendance)
    }
  }

  private fun attendanceAlreadyExistsFor(instance: ScheduledInstance, allocation: Allocation) =
    attendanceRepository.existsAttendanceByScheduledInstanceAndPrisonerNumber(instance, allocation.prisonerNumber)

  private fun lockAttendanceRecordsStartingFromYesterdayCountingBack() {
    log.info("Attempting to lock attendance records.")

    LocalDate.now().minusDays(1).let { yesterday ->
      val counter = AtomicInteger(0)

      // Note we are fetching a maximum of one months attendance records to avoid pulling back too much data.
      // Anything beyond 15 days from date of execution if not locked will be locked. Extending the number of days
      // retrieved to a month will help provide a recovery window should the job fail on a given day.
      forEachRolledOutPrison { prison ->
        attendanceRepository.findUnlockedAttendancesAtPrisonBetweenDates(
          prison.code,
          yesterday.minusMonths(1),
          yesterday,
        )
          .forEach { unlockedAttendance ->
            val dateOfAttendance = unlockedAttendance.scheduledInstance.sessionDate

            when {
              dateOfAttendance.between(yesterday.minusWeeks(2), yesterday) -> {
                unlockedAttendance.takeIf { it.status(AttendanceStatus.COMPLETED) }
              }

              else -> unlockedAttendance
            }?.let {
              runCatching {
                attendanceRepository.saveAndFlush(unlockedAttendance.lock())
              }.onFailure {
                log.error("Failed to lock attendance record ${unlockedAttendance.attendanceId}", it)
              }.onSuccess {
                counter.incrementAndGet()
              }
            }
          }
      }

      log.info("${counter.get()} attendance record(s) locked.")
    }
  }

  private fun forEachRolledOutPrison(block: (RolloutPrison) -> Unit) =
    rolloutPrisonRepository.findAll().filter { it.isActivitiesRolledOut() }.forEach { block(it) }
}

enum class AttendanceOperation {
  CREATE,
  LOCK,
}
