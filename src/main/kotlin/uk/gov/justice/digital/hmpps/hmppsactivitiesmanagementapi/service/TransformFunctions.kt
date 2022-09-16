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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityCategory as ModelActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityEligibility as ModelActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityInstance as ModelActivityInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPayBand as ModelActivityPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPrisoner as ModelActivityPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySession as ModelActivitySession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier as ModelActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EligibilityRule as ModelEligibilityRule

/**
 * Transform functions providing a thin layer to transform entities into their API equivalents and vice-versa.
 */
fun transform(activity: EntityActivity) =
  ModelActivity(
    id = activity.activityId!!,
    prisonCode = activity.prisonCode,
    category = activity.activityCategory.toModelActivityCategory(),
    tier = activity.activityTier.toModelActivityTier(),
    eligibilityRules = activity.eligibilityRules.toModelEligibilityRules(),
    sessions = activity.sessions.toModelSessions(),
    pay = activity.activityPay?.toModelActivityPay(),
    summary = activity.summary,
    description = activity.description,
    startDate = activity.startDate,
    endDate = activity.endDate,
    active = activity.active,
    createdAt = activity.createdAt,
    createdBy = activity.createdBy
  )

private fun EntityActivityCategory.toModelActivityCategory() = let {
  ModelActivityCategory(
    it.activityCategoryId!!,
    it.categoryCode,
    it.description
  )
}

private fun EntityActivityTier.toModelActivityTier() = let { ModelActivityTier(it.activityTier, it.description) }

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
    startTime = it.startTime,
    endTime = it.endTime,
    internalLocationId = it.internalLocationId,
    internalLocationCode = it.internalLocationCode,
    internalLocationDescription = it.internalLocationDescription,
    capacity = it.capacity,
    daysOfWeek = it.daysOfWeek
  )
}

private fun List<EntityActivityInstance>.toModelActivityInstances() = map {
  ModelActivityInstance(
    id = it.activityInstanceId!!,
    sessionDate = it.sessionDate,
    startTime = it.startTime,
    endTime = it.endTime,
    internalLocationId = it.internalLocationId,
    cancelled = it.cancelled,
    cancelledAt = it.cancelledAt,
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
    allocationAt = it.allocationAt,
    allocatedBy = it.allocatedBy
  )
}

private fun EntityActivityPay.toModelActivityPay() =
  ModelActivityPay(
    id = this.activityPayId!!,
    bands = this.payBands.toModelPayBands()
  )
