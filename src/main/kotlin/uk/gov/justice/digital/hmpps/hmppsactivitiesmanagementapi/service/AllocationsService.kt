package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonerAllocations
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation as ModelAllocation

@Service
class AllocationsService(private val allocationRepository: AllocationRepository, private val prisonPayBandRepository: PrisonPayBandRepository) {
  fun findByPrisonCodeAndPrisonerNumbers(prisonCode: String, prisonNumbers: Set<String>, activeOnly: Boolean = true) =
    allocationRepository
      .findByPrisonCodeAndPrisonerNumbers(prisonCode, prisonNumbers.toList())
      .filter { !activeOnly || it.status(PrisonerStatus.ACTIVE) }
      .toModelPrisonerAllocations()

  fun getAllocationById(id: Long) = allocationRepository.findOrThrowNotFound(id).toModel()

  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  fun updateAllocation(allocationId: Long, request: AllocationUpdateRequest, prisonCode: String): ModelAllocation {
    var allocation = allocationRepository.findOrThrowNotFound(allocationId)

    applyStartDateUpdate(request, allocation)
    applyEndDateUpdate(request, allocation)
    applyRemoveEndDateUpdate(request, allocation)
    applyPayBandUpdate(request, allocation)

    allocationRepository.saveAndFlush(allocation)

    return allocation.toModel()
  }

  private fun applyStartDateUpdate(
    request: AllocationUpdateRequest,
    allocation: Allocation,
  ) {
    request.startDate?.apply {
      if (allocation.startDate <= LocalDate.now()) {
        throw IllegalArgumentException("Start date cannot be updated once allocation has started")
      }
      if (this < allocation.activitySchedule.activity.startDate) {
        throw IllegalArgumentException("Allocation start date cannot be before activity start date")
      }
      allocation.startDate = this
    }
  }

  private fun applyEndDateUpdate(
    request: AllocationUpdateRequest,
    allocation: Allocation,
  ) {
    request.endDate?.apply {
      if (allocation.activitySchedule.activity.endDate !== null && this > allocation.activitySchedule.activity.endDate) {
        throw IllegalArgumentException("Allocation end date cannot be after activity end date")
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
}
