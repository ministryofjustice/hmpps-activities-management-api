package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance

@Service
class AttendancesService(
  private val scheduledInstanceRepository: ScheduledInstanceRepository,
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val attendanceHistoryRepository: AttendanceHistoryRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun findAttendancesByScheduledInstance(instanceId: Long) =
    scheduledInstanceRepository.findOrThrowNotFound(instanceId).attendances.map { transform(it) }

  // TODO this is a very thin slice when updating.
  // TODO some of the attributes still need populating as part of the marking journey e.g. recorded time/by, pay etc.
  // TODO also there is no validation checking.
  @PreAuthorize("hasAnyRole('ACTIVITY_ADMIN')")
  fun mark(attendances: List<AttendanceUpdateRequest>) {
    val attendanceUpdatesById = attendances.associateBy { it.id }
    val attendanceReasonsByCode = attendanceReasonRepository.findAll().associateBy { it.code.uppercase().trim() }

    val updatedAttendances = attendanceRepository.findAllById(attendanceUpdatesById.keys).mapNotNull {
      // For previously marked attendance records, we need to create a history record before updating the attendance
      this.createHistory(
        it,
      )
      it.mark(
        attendanceReasonsByCode[attendanceUpdatesById[it.attendanceId]!!.attendanceReason!!.uppercase().trim()],
        AttendanceStatus.COMPLETED,
        attendanceUpdatesById[it.attendanceId]!!.comment,
        attendanceUpdatesById[it.attendanceId]!!.issuePayment,
        attendanceUpdatesById[it.attendanceId]!!.incentiveLevelWarningIssued,
      )
    }

    attendanceRepository.saveAll(updatedAttendances)
  }

  /**
   * Create attendances on the given date for instances scheduled and allocations active on that date
   *
   * We do not need to worry about cancellations (at present) to schedules.
   */
  fun createAttendanceRecordsFor(date: LocalDate) {
    log.info("Creating attendance records for date: $date")

    scheduledInstanceRepository.findAllBySessionDate(date)
      .andAttendanceRequired()
      .forEach { instance ->
        instance.forEachActiveAllocation { allocation ->
          createAttendanceRecordIfNoPreExistingRecord(
            instance,
            allocation,
          )
        }
      }
  }

  private fun List<ScheduledInstance>.andAttendanceRequired() = filter { it.attendanceRequired() }

  private fun ScheduledInstance.forEachActiveAllocation(f: (allocation: Allocation) -> Unit) {
    activitySchedule.allocations().filter { it.status(PrisonerStatus.ACTIVE) }.forEach { f(it) }
  }

  // TODO not applying pay rates.
  private fun createAttendanceRecordIfNoPreExistingRecord(instance: ScheduledInstance, allocation: Allocation) {
    if (attendanceRepository.existsAttendanceByScheduledInstanceAndPrisonerNumber(
        instance,
        allocation.prisonerNumber,
      )
    ) {
      log.info("Attendance record already exists for allocation ${allocation.allocationId} and scheduled instance ${instance.scheduledInstanceId}")
      return
    }

    attendanceRepository.save(
      Attendance(
        scheduledInstance = instance,
        prisonerNumber = allocation.prisonerNumber,
      ),
    )
  }

  fun getAttendanceById(id: Long): ModelAttendance =
    attendanceRepository.findOrThrowNotFound(id).toModel()

  fun createHistory(
    attendance: Attendance,
  ) {
    if (attendance.status != AttendanceStatus.WAITING) {
      attendanceHistoryRepository.save(
        AttendanceHistory(
          attendance = attendance,
          attendanceReason = attendance.attendanceReason,
          comment = attendance.comment,
          recordedTime = attendance.recordedTime,
          recordedBy = attendance.recordedBy,
          issuePayment = attendance.issuePayment,
          caseNoteId = attendance.caseNoteId,
          incentiveLevelWarningIssued = attendance.incentiveLevelWarningIssued,
          otherAbsenceReason = attendance.otherAbsenceReason,
        ),
      )

    }
  }
}
