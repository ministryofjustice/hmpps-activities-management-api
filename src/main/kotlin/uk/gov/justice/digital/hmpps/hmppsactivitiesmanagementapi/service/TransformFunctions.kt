package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity as EntityActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory as EntityActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityEligibility as EntityActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityInstance as EntityActivityInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPay as EntityActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPayBand as EntityActivityPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPrisoner as EntityActivityPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySession as EntityActivitySession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier as EntityActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityWaiting as EntityActivityWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityCategory as ModelActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityEligibility as ModelActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityInstance as ModelActivityInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPayBand as ModelActivityPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPrisoner as ModelActivityPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySession as ModelActivitySession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier as ModelActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityWaiting as ModelActivityWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EligibilityRule as ModelEligibilityRule

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
    sessions = activity.sessions.toModelSessions(),
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

private fun List<EntityActivitySession>.toModelSessions() = map {
  ModelActivitySession(
    id = it.activitySessionId!!,
    instances = it.instances.toModelActivityInstances(),
    prisoners = it.prisoners.toModelActivityPrisoners(),
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

private fun List<EntityActivityWaiting>.toModelWaitingList() = map {
  ModelActivityWaiting(
    id = it.activityWaitingId!!,
    prisonerNumber = it.prisonerNumber,
    priority = it.priority,
    createdTime = it.createdTime,
    createdBy = it.createdBy,
  )
}

private fun List<EntityActivityInstance>.toModelActivityInstances() = map {
  ModelActivityInstance(
    id = it.activityInstanceId!!,
    sessionDate = it.sessionDate,
    startTime = it.startTime.toLocalTime(),
    endTime = it.endTime.toLocalTime(),
    cancelled = it.cancelled,
    cancelledTime = it.cancelledTime,
    cancelledBy = it.cancelledBy
  )
}

private fun List<EntityActivityPrisoner>.toModelActivityPrisoners() = map {
  ModelActivityPrisoner(
    id = it.activityPrisonerId!!,
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
