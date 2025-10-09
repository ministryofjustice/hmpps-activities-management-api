package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isActiveInPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

@Service
class ManageAllocationsService(
  private val allocationRepository: AllocationRepository,
  outboundEventsService: OutboundEventsService,
  monitoringService: MonitoringService,
  private val prisonerSearchApiApplicationClient: PrisonerSearchApiApplicationClient,
  private val prisonerReceivedHandler: PrisonerReceivedHandler,
) : ManageAllocationsBase(monitoringService, outboundEventsService) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun fixPrisonersIncorrectlyAutoSuspended() {
    allocationRepository.findActiveAllocations(PrisonerStatus.AUTO_SUSPENDED)
      .groupBy { it.prisonerNumber }
      .forEach { (prisonerNumber, allocations) ->
        prisonerSearchApiApplicationClient.findByPrisonerNumber(prisonerNumber)?.also { prisoner ->

          prisoner.let { prisoner ->
            allocations
              .map { it.prisonCode() }
              .firstOrNull { prisonCode -> prisoner.isActiveInPrison(prisonCode) }
              ?.let { prisonCode ->
                log.info("Fixing stuck auto-suspended allocation(s) for prisoner $prisonerNumber in prison $prisonCode")

                prisonerReceivedHandler.receivePrisoner(prisonCode, prisonerNumber)
              }
          }
        } ?: run {
          log.warn("Unable to find prisoner $prisonerNumber to fix stuck auto-suspended allocation(s).")
        }
      }
  }
}
