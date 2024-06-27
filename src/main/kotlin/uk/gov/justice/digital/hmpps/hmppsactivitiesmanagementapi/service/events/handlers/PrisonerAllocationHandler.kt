package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class PrisonerAllocationHandler(
  private val allocationRepository: AllocationRepository,
  private val attendanceRepository: AttendanceRepository,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  internal fun deallocate(prisonCode: String, prisonerNumber: String, reason: DeallocationReason) {
    deallocatePrisonerAndRemoveFutureAttendances(reason, prisonCode, prisonerNumber)
  }

  private fun deallocatePrisonerAndRemoveFutureAttendances(
    reason: DeallocationReason,
    prisonCode: String,
    prisonerNumber: String,
  ) {
    transactionHandler.newSpringTransaction {
      val updatedAttendanceIds = mutableSetOf<Long>()
      val allocations = allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
        prisonCode,
        prisonerNumber,
        *PrisonerStatus.allExcuding(PrisonerStatus.ENDED),
      )

      allocations.deallocateAffectedAllocations(reason, prisonCode, prisonerNumber)
        .removeFutureAttendances().let { updatedAttendanceIds.addAll(it) }

      allocationRepository.saveAllAndFlush(allocations)
      allocations to updatedAttendanceIds
    }.let { (allocations, updatedAttendanceIds) ->
      allocations.forEach {
          endedAllocation ->
        log.info("Sending prisoner allocation amended event for ended allocation ${endedAllocation.allocationId}")
        outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, endedAllocation.allocationId)
      }
      updatedAttendanceIds.forEach {
          attendanceId ->
        outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_DELETED, attendanceId)
      }.also {
        log.info("Sending attendance delete events")
      }
    }
  }

  private fun List<Allocation>.deallocateAffectedAllocations(
    reason: DeallocationReason,
    prisonCode: String,
    prisonerNumber: String,
  ) =
    onEach { it.deallocateNowWithReason(reason) }
      .also {
        log.info("Deallocated prisoner $prisonerNumber at prison $prisonCode from ${it.size} allocations.")
      }

  private fun List<Allocation>.removeFutureAttendances(): Set<Long> {
    val updatedAttendanceIds = mutableSetOf<Long>()
    val now = LocalDateTime.now()

    forEach { allocation ->
      attendanceRepository.findAttendancesOnOrAfterDateForAllocation(
        sessionDate = LocalDate.now(),
        activityScheduleId = allocation.activitySchedule.activityScheduleId,
        prisonerNumber = allocation.prisonerNumber,
      )
        .filter { attendance -> attendance.scheduledInstance.isEndFuture(now) }
        .onEach { futureAttendance ->
          log.info("Removing future attendance ${futureAttendance.attendanceId} for allocation ${allocation.allocationId}")
          futureAttendance.scheduledInstance.remove(futureAttendance)
          updatedAttendanceIds.add(futureAttendance.attendanceId)
        }
    }

    return updatedAttendanceIds
  }
}
