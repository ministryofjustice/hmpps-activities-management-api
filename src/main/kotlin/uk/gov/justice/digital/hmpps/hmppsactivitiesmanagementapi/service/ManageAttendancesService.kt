package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ManageAttendancesService(
  private val scheduledInstanceRepository: ScheduledInstanceRepository,
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun attendances(operation: AttendanceOperation) {
    when (operation) {
      AttendanceOperation.CREATE -> create(LocalDate.now())
      AttendanceOperation.LOCK -> TODO()
    }
  }

  private fun create(date: LocalDate) {
    log.info("Creating attendance records for date: $date")

    scheduledInstanceRepository.findAllBySessionDate(date)
      .andAttendanceRequired()
      .forEach { instance ->
        instance.forEachInFlightAllocation { allocation ->
          createAttendanceRecordIfNoPreExistingRecord(instance, allocation)
        }
      }
  }

  private fun List<ScheduledInstance>.andAttendanceRequired() = filter { it.attendanceRequired() }

  private fun ScheduledInstance.forEachInFlightAllocation(f: (allocation: Allocation) -> Unit) {
    activitySchedule.allocations().filterNot { it.status(PrisonerStatus.ENDED) }.forEach { f(it) }
  }

  private fun createAttendanceRecordIfNoPreExistingRecord(instance: ScheduledInstance, allocation: Allocation) {
    if (!attendanceAlreadyExistsFor(instance, allocation)) {
      val attendance = allocation
        .takeIf { it.status(PrisonerStatus.AUTO_SUSPENDED, PrisonerStatus.SUSPENDED) }
        ?.let {
          Attendance(
            scheduledInstance = instance,
            prisonerNumber = allocation.prisonerNumber,
            attendanceReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED),
            payAmount = instance.rateFor(allocation.payBand),
            issuePayment = false,
            status = AttendanceStatus.COMPLETED,
            recordedTime = LocalDateTime.now(),
            recordedBy = ServiceName.SERVICE_NAME.value,
          )
        } ?: Attendance(
        scheduledInstance = instance,
        prisonerNumber = allocation.prisonerNumber,
        payAmount = instance.rateFor(allocation.payBand),
      )

      attendanceRepository.saveAndFlush(attendance)
    }
  }

  fun attendanceAlreadyExistsFor(instance: ScheduledInstance, allocation: Allocation) =
    attendanceRepository.existsAttendanceByScheduledInstanceAndPrisonerNumber(instance, allocation.prisonerNumber)
}

enum class AttendanceOperation {
  CREATE,
  LOCK,
}
