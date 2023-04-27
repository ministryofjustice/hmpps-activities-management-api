package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance

@Service
class AttendancesService(
  private val scheduledInstanceRepository: ScheduledInstanceRepository,
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val caseNotesApiClient: CaseNotesApiClient,
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
  fun mark(principalName: String, attendances: List<AttendanceUpdateRequest>) {
    val attendanceUpdatesById = attendances.associateBy { it.id }
    val attendanceReasonsByCode = attendanceReasonRepository.findAll().associateBy { it.code.toString() }

    val updatedAttendances = attendanceRepository.findAllById(attendanceUpdatesById.keys).mapNotNull {
      var caseNoteDetails: CaseNote? = null
      if (!attendanceUpdatesById.containsKey(it.attendanceId)) { throw IllegalArgumentException("Attendance record not found") }
      if (!attendanceUpdatesById[it.attendanceId]!!.caseNote.isNullOrEmpty()) {
        caseNoteDetails = caseNotesApiClient.postCaseNote(attendanceUpdatesById[it.attendanceId]!!.prisonCode, it.prisonerNumber, attendanceUpdatesById[it.attendanceId]!!.caseNote!!, attendanceUpdatesById[it.attendanceId]!!.incentiveLevelWarningIssued!!)
      }
      it.mark(
        principalName,
        attendanceReasonsByCode[attendanceUpdatesById[it.attendanceId]!!.attendanceReason?.uppercase()?.trim()],
        attendanceUpdatesById[it.attendanceId]?.status ?: AttendanceStatus.COMPLETED,
        attendanceUpdatesById[it.attendanceId]!!.comment,
        attendanceUpdatesById[it.attendanceId]!!.issuePayment,
        attendanceUpdatesById[it.attendanceId]!!.payAmount,
        attendanceUpdatesById[it.attendanceId]!!.incentiveLevelWarningIssued,
        caseNoteDetails?.caseNoteId,
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
        instance.forEachInFlightAllocation { allocation ->
          createAttendanceRecordIfNoPreExistingRecord(instance, allocation)
        }
      }
  }

  private fun List<ScheduledInstance>.andAttendanceRequired() = filter { it.attendanceRequired() }

  private fun ScheduledInstance.forEachInFlightAllocation(f: (allocation: Allocation) -> Unit) {
    activitySchedule.allocations().filterNot { it.status(PrisonerStatus.ENDED) }.forEach { f(it) }
  }

  // TODO not applying pay rates.
  private fun createAttendanceRecordIfNoPreExistingRecord(
    instance: ScheduledInstance,
    allocation: Allocation,
  ) {
    if (attendanceRepository.existsAttendanceByScheduledInstanceAndPrisonerNumber(
        instance,
        allocation.prisonerNumber,
      )
    ) {
      log.info("Attendance record already exists for allocation ${allocation.allocationId} and scheduled instance ${instance.scheduledInstanceId}")
      return
    }

    if (allocation.status(PrisonerStatus.AUTO_SUSPENDED, PrisonerStatus.SUSPENDED)) {
      val suspendedReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)

      attendanceRepository.saveAndFlush(
        Attendance(
          scheduledInstance = instance,
          prisonerNumber = allocation.prisonerNumber,
          attendanceReason = suspendedReason,
          issuePayment = false,
          status = AttendanceStatus.COMPLETED,
          recordedTime = LocalDateTime.now(),
          recordedBy = "Activities Management Service",
        ),
      )

      return
    }

    attendanceRepository.saveAndFlush(
      Attendance(
        scheduledInstance = instance,
        prisonerNumber = allocation.prisonerNumber,
      ),
    )
  }

  fun getAttendanceById(id: Long): ModelAttendance =
    attendanceRepository.findOrThrowNotFound(id).toModel(caseNotesApiClient)
}
