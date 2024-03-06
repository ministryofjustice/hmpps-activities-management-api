package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isActiveInPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceSuspensionDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.isActivitiesRolledOutAt
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReceivedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDateTime

/**
 * Handler is responsible for un-suspending any auto-suspended allocations and suspended attendance records matching
 * the prison and prisoner for the particular offender received domain event. Note is will not unsuspend manually
 * suspended allocations.
 *
 * Will also raise allocation and attendance amended events for all allocation and attendance records updated as a
 * result.
 */
@Component
@Transactional
class OffenderReceivedEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val allocationRepository: AllocationRepository,
  private val prisonerSearchApiApplicationClient: PrisonerSearchApiApplicationClient,
  private val attendanceSuspensionDomainService: AttendanceSuspensionDomainService,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
) : EventHandler<OffenderReceivedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: OffenderReceivedEvent): Outcome {
    log.debug("Handling offender received event {}", event)

    if (rolloutPrisonRepository.isActivitiesRolledOutAt(event.prisonCode())) {
      prisonerSearchApiApplicationClient.findByPrisonerNumber(event.prisonerNumber())?.let { prisoner ->
        if (prisoner.isActiveInPrison(event.prisonCode())) {
          transactionHandler.newSpringTransaction {
            allocationRepository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
              .resetAutoSuspendedAllocations(event)
              .resetFutureAutoSuspendedAttendances()
          }.let { resetAllocations ->
            resetAllocations.forEach {
              outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, it.first.allocationId)
              it.second.forEach { attendance ->
                outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
              }
            }.also { log.info("Sending allocation amended events.") }
          }
        } else {
          log.info("Prisoner ${event.prisonerNumber()} is not active in prison ${event.prisonCode()}")
        }

        return Outcome.success()
      }
    }

    log.debug("Ignoring received event for ${event.prisonCode()} - not rolled out.")

    return Outcome.success()
  }

  private fun List<Allocation>.resetAutoSuspendedAllocations(event: OffenderReceivedEvent) =
    this.filter { it.status(PrisonerStatus.AUTO_SUSPENDED) }
      .onEach {
        if (it.isCurrentlySuspended()) {
          it.activatePlannedSuspension()
        } else {
          it.reactivateSuspension()
        }
      }
      .also {
        log.info("Reset ${it.size} suspended allocations for prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()}.")
      }

  private fun List<Allocation>.resetFutureAutoSuspendedAttendances() =
    map { it to attendanceSuspensionDomainService.resetAutoSuspendedFutureAttendancesForAllocation(LocalDateTime.now(), it) }
}
