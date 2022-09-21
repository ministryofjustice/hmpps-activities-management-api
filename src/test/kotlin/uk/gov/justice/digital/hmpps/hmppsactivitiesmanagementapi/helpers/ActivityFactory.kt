package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.transform
import java.time.LocalDate
import java.time.LocalDateTime

internal fun activityModel(activity: Activity) = transform(activity)

internal fun activityEntity(
  category: ActivityCategory = activityCategory(),
  tier: ActivityTier = activityTier(),
  timestamp: LocalDateTime = LocalDate.now().atStartOfDay(),
) =
  Activity(
    activityId = 1,
    prisonCode = "123",
    activityCategory = category,
    activityTier = tier,
    summary = "Maths",
    description = "Maths basic",
    startDate = timestamp.toLocalDate(),
    createdTime = timestamp,
    createdBy = "test"
  ).apply {
    eligibilityRules.add(activityEligibilityRule(this))
    sessions.add(activitySession(this, timestamp))
    waitingList.add(activityWaiting(this, timestamp))
    activityPay = activityPay(this)
  }

private fun activityCategory() =
  ActivityCategory(activityCategoryId = 1, code = "code", description = "category description")

private fun activityTier() = ActivityTier(activityTierId = 1, code = "T1", description = "Tier 1")

private fun activityEligibilityRule(activity: Activity): ActivityEligibility {
  val eligibilityRule = EligibilityRule(eligibilityRuleId = 1, code = "code", "rule description")

  return ActivityEligibility(
    activityEligibilityId = 1,
    activity = activity,
    eligibilityRule = eligibilityRule
  )
}

private fun activitySession(
  activity: Activity,
  timestamp: LocalDateTime
) =
  ActivitySession(
    activitySessionId = 1,
    activity = activity,
    description = "session description",
    startTime = timestamp,
    endTime = timestamp,
    capacity = 1,
    daysOfWeek = "0000001"
  ).apply {
    this.instances.add(
      ActivityInstance(
        activityInstanceId = 1,
        activitySession = this,
        sessionDate = timestamp.toLocalDate(),
        startTime = timestamp,
        endTime = timestamp
      )
    )
  }

private fun activityWaiting(
  activity: Activity,
  timestamp: LocalDateTime
) =
  ActivityWaiting(
    activityWaitingId = 1,
    activity = activity,
    prisonerNumber = "1234567890",
    priority = 1,
    createdTime = timestamp,
    createdBy = "test"
  )

private fun activityPay(activity: Activity) =
  ActivityPay(
    activityPayId = 1,
    activity = activity
  ).apply {
    payBands.add(activityPayBand(this))
  }

private fun activityPayBand(activityPay: ActivityPay) =
  ActivityPayBand(
    activityPayBandId = 1,
    activityPay = activityPay
  )
