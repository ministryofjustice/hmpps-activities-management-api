package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity as EntityActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory as EntityActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityEligibility as EntityActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPay as EntityActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPayBand as EntityActivityPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule as EntityActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier as EntityActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation as EntityAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerWaiting as EntityPrisonerWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance as EntityScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityCategory as ModelActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityEligibility as ModelActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPayBand as ModelActivityPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier as ModelActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation as ModelAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EligibilityRule as ModelEligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerWaiting as ModelPrisonerWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstance as ModelScheduledInstance

/**
 * Transform functions providing a thin layer to transform entities into their API model equivalents and vice-versa.
 */
fun transform(activity: EntityActivity) =
  ModelActivity(
    id = activity.activityId!!,
    prisonCode = activity.prisonCode,
    category = activity.activityCategory.toModelActivityCategory(),
    tier = activity.activityTier.toModelActivityTier(),
    eligibilityRules = activity.eligibilityRules.toModelEligibilityRules(),
    schedules = activity.schedules.toModelSchedules(),
    waitingList = activity.waitingList.toModelWaitingList(),
    pay = activity.activityPay?.toModelActivityPay(),
    summary = activity.summary,
    description = activity.description,
    startDate = activity.startDate,
    endDate = activity.endDate,
    active = activity.active,
    createdTime = activity.createdTime,
    createdBy = activity.createdBy
  )

private fun EntityActivityCategory.toModelActivityCategory() =
  ModelActivityCategory(
    this.activityCategoryId!!,
    this.code,
    this.description
  )

private fun EntityActivityTier.toModelActivityTier() =
  ModelActivityTier(
    id = this.activityTierId!!,
    code = this.code,
    description = this.description,
  )

private fun List<EntityActivityEligibility>.toModelEligibilityRules() = map {
  ModelActivityEligibility(
    it.activityEligibilityId!!,
    it.eligibilityRule.let { er -> ModelEligibilityRule(er.eligibilityRuleId!!, er.code, er.description) }
  )
}

private fun List<EntityActivityPayBand>.toModelPayBands() = map {
  ModelActivityPayBand(
    id = it.activityPayBandId!!,
    payBand = it.payBand,
    rate = it.rate,
    pieceRate = it.pieceRate,
    pieceRateItems = it.pieceRateItems
  )
}

private fun List<EntityActivitySchedule>.toModelSchedules() = map {
  ModelActivitySchedule(
    id = it.activityScheduleId!!,
    instances = it.instances.toModelScheduledInstances(),
    allocations = it.allocations.toModelAllocations(),
    description = it.description,
    suspendUntil = it.suspendUntil,
    startTime = it.startTime.toLocalTime(),
    endTime = it.endTime.toLocalTime(),
    internalLocationId = it.internalLocationId,
    internalLocationCode = it.internalLocationCode,
    internalLocationDescription = it.internalLocationDescription,
    capacity = it.capacity,
    daysOfWeek = it.daysOfWeek
  )
}

private fun List<EntityPrisonerWaiting>.toModelWaitingList() = map {
  ModelPrisonerWaiting(
    id = it.prisonerWaitingId!!,
    prisonerNumber = it.prisonerNumber,
    priority = it.priority,
    createdTime = it.createdTime,
    createdBy = it.createdBy,
  )
}

private fun List<EntityScheduledInstance>.toModelScheduledInstances() = map {
  ModelScheduledInstance(
    id = it.scheduledInstanceId!!,
    date = it.sessionDate,
    startTime = it.startTime.toLocalTime(),
    endTime = it.endTime.toLocalTime(),
    cancelled = it.cancelled,
    cancelledTime = it.cancelledTime,
    cancelledBy = it.cancelledBy
  )
}

private fun List<EntityAllocation>.toModelAllocations() = map {
  ModelAllocation(
    id = it.allocationId!!,
    prisonerNumber = it.prisonerNumber,
    iepLevel = it.iepLevel,
    payBand = it.payBand,
    startDate = it.startDate,
    endDate = it.endDate,
    active = it.active,
    allocatedTime = it.allocatedTime,
    allocatedBy = it.allocatedBy
  )
}

private fun EntityActivityPay.toModelActivityPay() =
  ModelActivityPay(
    id = this.activityPayId!!,
    bands = this.payBands.toModelPayBands(),
    iepBasicRate = this.iepBasicRate,
    iepStandardRate = this.iepStandardRate,
    iepEnhancedRate = this.iepEnhancedRate,
    pieceRate = this.pieceRate,
    pieceRateItems = this.pieceRateItems
  )
