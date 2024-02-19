package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isAtDifferentLocationTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isInactiveOut
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.lastMovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason.OTHER
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason.RELEASED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason.TEMPORARILY_RELEASED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.isActivitiesRolledOutAt
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendanceSuspensionDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.Action
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ActivitiesChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalDateTime

@Component
@Transactional
class ActivitiesChangedEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val allocationRepository: AllocationRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient,
  private val allocationHandler: PrisonerAllocationHandler,
  private val transactionHandler: TransactionHandler,
  private val waitingListService: WaitingListService,
  private val outboundEventsService: OutboundEventsService,
  private val attendanceSuspensionDomainService: AttendanceSuspensionDomainService,
) : EventHandler<ActivitiesChangedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: ActivitiesChangedEvent): Outcome {
    log.debug("Handling activities changed event {}", event)

    if (rolloutPrisonRepository.isActivitiesRolledOutAt(event.prisonCode())) {
      return when (event.action()) {
        Action.SUSPEND -> suspendPrisonerAllocationsAndAttendances(event).let { Outcome.success() }
        Action.END -> {
          waitingListService.removeOpenApplications(event.prisonCode(), event.prisonerNumber(), ServiceName.SERVICE_NAME.value)
          if (prisonerHasAllocationsOfInterestFor(event)) {
            allocationHandler.deallocate(event.prisonCode(), event.prisonerNumber(), getDeallocationReasonFor(event))
          } else {
            log.info("No allocations of interest for prisoner ${event.prisonerNumber()}")
          }
          Outcome.success()
        }

        else -> Outcome.failed().also { log.warn("Unable to process $event, unknown action") }
      }
    }

    return Outcome.success()
  }

  private fun prisonerHasAllocationsOfInterestFor(event: ActivitiesChangedEvent) =
    allocationRepository.existAtPrisonForPrisoner(
      event.prisonCode(),
      event.prisonerNumber(),
      PrisonerStatus.allExcuding(PrisonerStatus.ENDED).toList(),
    )

  private fun suspendPrisonerAllocationsAndAttendances(event: ActivitiesChangedEvent) =
    LocalDateTime.now().let { now ->
      transactionHandler.newSpringTransaction {
        allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
          event.prisonCode(),
          event.prisonerNumber(),
          PrisonerStatus.ACTIVE,
          PrisonerStatus.PENDING,
          PrisonerStatus.SUSPENDED,
        )
          .excludingFuturePendingAllocations()
          .autoSuspendPrisonersAllocations(now, event)
          .map { it to attendanceSuspensionDomainService.suspendFutureAttendancesForAllocation(now, it) }
      }.let { suspendedAllocations ->
        suspendedAllocations.forEach {
          outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, it.first.allocationId)
          it.second.forEach { attendance ->
            outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
          }
        }.also { log.info("Sending allocation amended events.") }
      }
    }

  private fun List<Allocation>.excludingFuturePendingAllocations() =
    filterNot { it.prisonerStatus == PrisonerStatus.PENDING && it.startDate.isAfter(LocalDate.now()) }

  private fun List<Allocation>.autoSuspendPrisonersAllocations(suspendedAt: LocalDateTime, event: ActivitiesChangedEvent) =
    onEach { it.autoSuspend(suspendedAt, "Temporarily released or transferred") }
      .also { log.info("Auto suspended ${it.size} allocations for prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()}.") }

  private fun getDeallocationReasonFor(event: ActivitiesChangedEvent) =
    prisonerSearchApiClient.findByPrisonerNumber(event.prisonerNumber())
      .throwNullPointerIfNotFound { "Prisoner search lookup failed for prisoner ${event.prisonerNumber()}" }
      .let { prisoner ->
        when {
          prisoner.isInactiveOut() -> RELEASED.also { log.info("Released inactive out prisoner ${event.prisonerNumber()} from prison ${event.prisonCode()}") }
          prisoner.isAtDifferentLocationTo(event.prisonCode()) -> TEMPORARILY_RELEASED.also { log.info("Temporary release or transfer of prisoner ${event.prisonerNumber()} from prison ${event.prisonCode()}") }
          // The user has explicitly chosen END, so we know they want to end but cannot easily determine a reason. If we don't it causes issues for the prisons.
          else -> OTHER.also {
            log.info("Prisoner prison code '${prisoner.prisonId}', prisoner '${prisoner.prisonerNumber}', status '${prisoner.status}', last movement type code '${prisoner.lastMovementType()}'")
            log.info("Defaulting to OTHER for deallocation reason for prisoner ${event.prisonerNumber()} from prison ${event.prisonCode()}")
          }
        }
      }

  private fun Prisoner?.throwNullPointerIfNotFound(message: () -> String): Prisoner {
    if (this == null) {
      throw NullPointerException(message())
    }

    return this
  }
}
