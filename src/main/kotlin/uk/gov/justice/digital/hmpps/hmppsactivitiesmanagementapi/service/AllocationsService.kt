package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ExclusionsFilter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
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
  private val manageAttendancesService: ManageAttendancesService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun findByPrisonCodeAndPrisonerNumbers(prisonCode: String, prisonNumbers: Set<String>, activeOnly: Boolean = true) = allocationRepository
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

      val newAttendances = manageAttendancesService.createAnyAttendancesForToday(
        allocation = allocation,

        scheduleInstanceId = request.scheduleInstanceId,
        firstTimeSlot = request.firstTimeSlotForToday,
      )

      val savedAttendances = manageAttendancesService.saveAttendances(newAttendances, allocation.activitySchedule.description)

      allocation.toModel() to savedAttendances
    }.let { (allocation, newAttendances) ->

      log.info("Sending allocation amended event for allocation ${allocation.id}")

      outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocation.id)

      newAttendances.forEach { manageAttendancesService.sendCreatedEvent(it) }

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
    val todayScheduledInstance = request.firstTimeSlotForToday?.let { timeSlot ->
      allocation.activitySchedule.instances().first { it.sessionDate == LocalDate.now() && it.timeSlot == timeSlot }
    }

    val today = LocalDate.now()

    request.exclusions?.let { exclusions ->
      /*
       End any exclusions that have started and not ended.

       Although Nomis does not have the concept of exclusion date ranges the old exclusions are needed for historical
       reasons such as unlock and attendance lists (SAA-1379)
       */
      val newExclusionSlots = exclusions.map { it.weekNumber to it.timeSlot }.toSet()

      // Remove exclusions staring in the future that are not in the request
      allocation.removeExclusions(
        allocation.exclusions(ExclusionsFilter.FUTURE)
          .filter { (it.weekNumber to it.timeSlot) !in newExclusionSlots }
          .toSet(),
      )

      // When the update request includes a scheduled instance for today, then end as yesterday or remove
      if (todayScheduledInstance != null) {
        val (toRemove, toEnd) = allocation.exclusions(ExclusionsFilter.PRESENT)
          .filter { it.timeSlot >= todayScheduledInstance.timeSlot }
          .partition { it.startDate == today }

        allocation.removeExclusions(toRemove.toSet())
        allocation.endExclusionsYesterday(toEnd.toSet())
      }

      // End any remaining exclusions that are current
      allocation.endExclusions(allocation.exclusions(ExclusionsFilter.PRESENT))

      exclusions.forEach { exclusion ->
        require(!allocation.activitySchedule.noMatchingSlots(exclusion)) {
          "Updating allocation with id ${allocation.allocationId}: No ${exclusion.timeSlot} slots in week number ${exclusion.weekNumber}"
        }

        val startDate = todayScheduledInstance
          ?.takeIf { it.timeSlot <= exclusion.timeSlot && exclusion.daysOfWeek.contains(today.dayOfWeek) }
          ?.let { today }
          ?: today.plusDays(1)

        // exclusion updates always apply tomorrow if the allocation date is in the past (as per the UI content)
        allocation.updateExclusion(
          exclusionSlot = exclusion,
          startDate = maxOf(allocation.startDate, startDate),
        )
      }
    }
  }

  private fun applyStartDateUpdate(request: AllocationUpdateRequest, allocation: Allocation) = request.startDate?.let { newStartDate -> updateStartDate(allocation, newStartDate, request.scheduleInstanceId) }

  @Transactional(propagation = Propagation.REQUIRED, noRollbackFor = [IllegalArgumentException::class])
  fun updateStartDateIgnoringValidationErrors(allocation: Allocation, startDate: LocalDate) = updateStartDate(allocation, startDate)

  private fun updateStartDate(allocation: Allocation, startDate: LocalDate, scheduleInstanceId: Long? = null) {
    startDate.let { newStartDate ->
      val (start, end) = allocation.activitySchedule.activity.startDate to allocation.activitySchedule.activity.endDate

      val today = LocalDate.now()

      require(startDate >= today) { "Allocation start date must not be in the past" }

      if (startDate == today && scheduleInstanceId == null) throw IllegalArgumentException("The next session must be provided when allocation start date is today")

      require(allocation.startDate > today) {
        "Start date cannot be updated once allocation has started"
      }

      require(newStartDate.between(start, end)) {
        "Allocation start date cannot be before the activity start date or after the activity end date."
      }

      require(allocation.endDate == null || newStartDate <= allocation.endDate) {
        "Allocation start date cannot be after allocation end date"
      }

      allocation.prisonerStatus = if (newStartDate.isAfter(LocalDate.now())) PrisonerStatus.PENDING else PrisonerStatus.ACTIVE
      allocation.startDate = newStartDate
      allocation.removeRedundantAdvanceAttendances(allocation.startDate, allocation.endDate)
      allocation.exclusions(ExclusionsFilter.FUTURE).forEach { it.startDate = newStartDate }
    }
  }

  fun applyEndDateUpdate(request: AllocationUpdateRequest, allocation: Allocation, updatedBy: String) {
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
      allocation.removeRedundantAdvanceAttendances(allocation.startDate, allocation.endDate)
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

  private fun String?.toDeallocationReason() = DeallocationReason.entries
    .filter(DeallocationReason::displayed)
    .firstOrNull { it.name == this } ?: throw IllegalArgumentException("Invalid deallocation reason specified '$this'")
}
