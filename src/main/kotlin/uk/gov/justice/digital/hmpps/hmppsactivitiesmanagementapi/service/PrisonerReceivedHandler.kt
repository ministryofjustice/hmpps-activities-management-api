package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDateTime

@Service
class PrisonerReceivedHandler(
  private val allocationRepository: AllocationRepository,
  private val attendanceSuspensionDomainService: AttendanceSuspensionDomainService,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun receivePrisoner(prisonCode: String, prisonerNumber: String) {
    transactionHandler.newSpringTransaction {
      allocationRepository.findByPrisonCodeAndPrisonerNumber(prisonCode, prisonerNumber)
        .resetAutoSuspendedAllocations(prisonCode, prisonerNumber)
        .resetFutureAutoSuspendedAttendances()
    }.let { resetAllocations ->
      resetAllocations.forEach { (allocation, attendances) ->
        outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocation.allocationId)
        attendances.forEach { attendance ->
          outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
        }
      }.also { log.info("PRISONER RECEIVED: sending allocation amended events.") }
    }
  }

  private fun List<Allocation>.resetAutoSuspendedAllocations(prisonCode: String, prisonerNumber: String) = this.filter {
    it.status(
      PrisonerStatus.AUTO_SUSPENDED,
    )
  }
    .onEach {
      if (it.isCurrentlySuspended()) {
        log.info("PRISONER RECEIVED: re-activating planned suspension for prisoner $prisonerNumber at prison $prisonCode for allocation ${it.allocationId}.")
        it.activatePlannedSuspension()
      } else {
        log.info("PRISONER RECEIVED: removing auto-suspension for prisoner $prisonerNumber at prison $prisonCode for allocation ${it.allocationId}.")
        it.reactivateSuspension()
      }
    }
    .also {
      log.info("PRISONER RECEIVED: reset ${it.size} suspended allocations for prisoner $prisonerNumber at prison $prisonCode.")
    }

  private fun List<Allocation>.resetFutureAutoSuspendedAttendances() = map {
    it to attendanceSuspensionDomainService.resetAutoSuspendedFutureAttendancesForAllocation(
      LocalDateTime.now(),
      it,
    )
  }
}
