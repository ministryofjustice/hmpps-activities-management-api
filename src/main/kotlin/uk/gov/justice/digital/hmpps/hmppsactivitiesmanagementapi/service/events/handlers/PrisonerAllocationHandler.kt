package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class PrisonerAllocationHandler(
  private val allocationRepository: AllocationRepository,
  private val attendanceRepository: AttendanceRepository,
  private val waitingListService: WaitingListService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  internal fun deallocate(prisonCode: String, prisonerNumber: String, reason: DeallocationReason) {
    declinePrisonersWaitingListApplications(prisonCode, prisonerNumber)
    deallocatePrisonerAndRemoveFutureAttendances(reason, prisonCode, prisonerNumber)
  }

  private fun declinePrisonersWaitingListApplications(prisonCode: String, prisonerNumber: String) {
    waitingListService.declinePendingOrApprovedApplications(
      prisonCode,
      prisonerNumber,
      "Released",
      ServiceName.SERVICE_NAME.value,
    )
  }

  private fun deallocatePrisonerAndRemoveFutureAttendances(
    reason: DeallocationReason,
    prisonCode: String,
    prisonerNumber: String,
  ) =
    allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
      prisonCode,
      prisonerNumber,
      *PrisonerStatus.allExcuding(PrisonerStatus.ENDED),
    )
      .deallocateAffectedAllocations(reason, prisonCode, prisonerNumber)
      .removeFutureAttendances(prisonCode)

  private fun List<Allocation>.deallocateAffectedAllocations(
    reason: DeallocationReason,
    prisonCode: String,
    prisonerNumber: String,
  ) =
    onEach { it.deallocateNowWithReason(reason) }
      .also {
        log.info("Deallocated prisoner $prisonerNumber at prison $prisonCode from ${it.size} allocations.")
      }

  private fun List<Allocation>.removeFutureAttendances(prisonCode: String) {
    val now = LocalDateTime.now()

    forEach { allocation ->
      attendanceRepository.findAttendancesOnOrAfterDateForPrisoner(
        prisonCode = prisonCode,
        sessionDate = LocalDate.now(),
        prisonerNumber = allocation.prisonerNumber,
      ).filter { attendance ->
        (attendance.scheduledInstance.sessionDate == now.toLocalDate() && attendance.scheduledInstance.startTime > now.toLocalTime()) ||
          (attendance.scheduledInstance.sessionDate > now.toLocalDate())
      }.onEach { futureAttendance ->
        futureAttendance.scheduledInstance.remove(futureAttendance)
      }
    }
  }
}
