package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ExclusionsFilter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonerAllocations
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation as ModelAllocation

@Service
@Transactional(readOnly = true)
class AllocationsService(
  private val allocationRepository: AllocationRepository,
  private val prisonPayBandRepository: PrisonPayBandRepository,
  private val scheduleRepository: ActivityScheduleRepository,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun findByPrisonCodeAndPrisonerNumbers(prisonCode: String, prisonNumbers: Set<String>, activeOnly: Boolean = true) =
    allocationRepository
      .findByPrisonCodeAndPrisonerNumbers(prisonCode, prisonNumbers.toList())
      .filter { !activeOnly || !it.status(PrisonerStatus.ENDED) }
      .toModelPrisonerAllocations()

  fun getAllocationById(id: Long): ModelAllocation {
    val allocation = allocationRepository.findOrThrowNotFound(id).toModel()
    val schedule = scheduleRepository.findOrThrowNotFound(allocation.scheduleId)
    checkCaseloadAccess(schedule.activity.prisonCode)

    return allocation
  }

  @Transactional
  fun updateAllocation(allocationId: Long, request: AllocationUpdateRequest, prisonCode: String, updatedBy: String): ModelAllocation {
    transactionHandler.newSpringTransaction {
      val allocation =
        allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)
          ?: throw EntityNotFoundException("Allocation $allocationId not found at $prisonCode.")

      require(
        allocation.status(PrisonerStatus.ENDED).not(),
      ) { "Ended allocations cannot be updated" }

      applyStartDateUpdate(request, allocation)
      applyEndDateUpdate(request, allocation, updatedBy)
      applyRemoveEndDateUpdate(request, allocation)
      applyPayBandUpdate(request, allocation)
      applyReasonCode(request, allocation, updatedBy)
      applyExclusionsUpdate(request, allocation)

      allocationRepository.saveAndFlush(allocation)

      allocation.toModel()
    }.let { allocation ->
      log.info("Sending allocation amended event for allocation ${allocation.id}")
      outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocation.id)
      return allocation
    }
  }

  private fun applyReasonCode(
    request: AllocationUpdateRequest,
    allocation: Allocation,
    updatedBy: String,
  ) {
    request.reasonCode?.apply {
      allocation.activitySchedule.deallocatePrisonerOn(allocation.prisonerNumber, allocation.endDate!!, this.toDeallocationReason(), updatedBy)
    }
  }

  private fun applyExclusionsUpdate(request: AllocationUpdateRequest, allocation: Allocation) {
    request.exclusions?.apply {
      allocation.endExclusions(allocation.exclusions(ExclusionsFilter.PRESENT))

      val newExclusions = this.map { ex -> ex.weekNumber to ex.timeSlot }
      val exclusionsToRemove = allocation.exclusions(ExclusionsFilter.FUTURE).mapNotNull {
        val oldExclusion = it.weekNumber to it.timeSlot().toString()
        it.takeIf { oldExclusion !in newExclusions }
      }.toSet()
      allocation.removeExclusions(exclusionsToRemove)
    }

    request.exclusions?.onEach { exclusion ->
      allocation.activitySchedule.slots(exclusion.weekNumber, exclusion.timeSlot())
        .also { require(it.isNotEmpty()) { "Updating allocation with id ${allocation.allocationId}: No ${exclusion.timeSlot()} slots in week number ${exclusion.weekNumber}" } }
        .filter { slot -> slot.getDaysOfWeek().intersect(exclusion.getDaysOfWeek()).isNotEmpty() }
        .forEach { slot -> allocation.updateExclusion(slot, exclusion.getDaysOfWeek()) }
    }
  }

  private fun applyStartDateUpdate(
    request: AllocationUpdateRequest,
    allocation: Allocation,
  ) {
    request.startDate?.let { newStartDate ->
      val (start, end) = allocation.activitySchedule.activity.startDate to allocation.activitySchedule.activity.endDate

      require(request.startDate > LocalDate.now()) { "Allocation start date must be in the future" }

      require(allocation.startDate > LocalDate.now()) {
        "Start date cannot be updated once allocation has started"
      }

      require(newStartDate.between(start, end)) {
        "Allocation start date cannot be before the activity start date or after the activity end date."
      }

      require(allocation.endDate == null || newStartDate <= allocation.endDate) {
        "Allocation start date cannot be after allocation end date"
      }

      allocation.startDate = newStartDate
    }
  }

  private fun applyEndDateUpdate(
    request: AllocationUpdateRequest,
    allocation: Allocation,
    updatedBy: String,
  ) {
    request.endDate?.apply {
      require(allocation.endDate != null || request.reasonCode != null) {
        "Reason code must be supplied when setting the allocation end date"
      }
      require(allocation.activitySchedule.activity.endDate == null || this <= allocation.activitySchedule.activity.endDate) {
        "Allocation end date cannot be after activity end date"
      }
      if (allocation.endDate == null) {
        allocation.activitySchedule.deallocatePrisonerOn(allocation.prisonerNumber, this, request.reasonCode.toDeallocationReason(), updatedBy)
      } else {
        allocation.activitySchedule.deallocatePrisonerOn(allocation.prisonerNumber, this, allocation.plannedDeallocation!!.plannedReason, updatedBy)
      }
      allocation.endDate = this
    }
  }

  private fun applyRemoveEndDateUpdate(
    request: AllocationUpdateRequest,
    allocation: Allocation,
  ) {
    request.removeEndDate?.apply {
      if (this) {
        allocation.endDate = null
      }
    }
  }

  private fun applyPayBandUpdate(
    request: AllocationUpdateRequest,
    allocation: Allocation,
  ) {
    request.payBandId?.apply {
      allocation.payBand = prisonPayBandRepository.findOrThrowIllegalArgument(this)
    }
  }

  private fun String?.toDeallocationReason() =
    DeallocationReason.entries
      .filter(DeallocationReason::displayed)
      .firstOrNull { it.name == this } ?: throw IllegalArgumentException("Invalid deallocation reason specified '$this'")
}
