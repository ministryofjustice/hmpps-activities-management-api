package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
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
    schedules.add(activitySchedule(this, timestamp))
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

private fun activitySchedule(
  activity: Activity,
  timestamp: LocalDateTime
) =
  ActivitySchedule(
    activityScheduleId = 1,
    activity = activity,
    description = "schedule description",
    startTime = timestamp.toLocalTime(),
    endTime = timestamp.toLocalTime(),
    capacity = 1,
    daysOfWeek = "0000001",
    internalLocationId = 1,
    internalLocationCode = "EDU-ROOM-1",
    internalLocationDescription = "Education - R1"
  ).apply {
    this.instances.add(
      ScheduledInstance(
        scheduledInstanceId = 1,
        activitySchedule = this,
        sessionDate = timestamp.toLocalDate(),
        startTime = timestamp.toLocalTime(),
        endTime = timestamp.toLocalTime()
      ).apply {
        this.attendances.add(
          Attendance(
            attendanceId = 1,
            scheduledInstance = this,
            prisonerNumber = "A11111A",
            posted = false
          )
        )
      }
    )
    this.allocations.add(
      Allocation(
        allocationId = 1,
        activitySchedule = this,
        prisonerNumber = "A1234AA",
        iepLevel = "BAS",
        payBand = "A",
        startDate = timestamp.toLocalDate(),
        endDate = null,
        active = true,
        allocatedTime = timestamp,
        allocatedBy = "Mr Blogs",
      )
    )
  }

private fun activityWaiting(
  activity: Activity,
  timestamp: LocalDateTime
) =
  PrisonerWaiting(
    prisonerWaitingId = 1,
    activity = activity,
    prisonerNumber = "1234567890",
    priority = 1,
    createdTime = timestamp,
    createdBy = "test"
  )

private fun activityPay(activity: Activity) =
  ActivityPay(
    activityPayId = 1,
    activity = activity,
    iepBasicRate = 10,
    iepStandardRate = 20,
    iepEnhancedRate = 30,
    pieceRate = 40,
    pieceRateItems = 50
  ).apply {
    payBands.add(activityPayBand(this))
  }

private fun activityPayBand(activityPay: ActivityPay) =
  ActivityPayBand(
    activityPayBandId = 1,
    activityPay = activityPay
  )

fun rolloutPrison() = RolloutPrison(1, "PVI", "HMP Pentonville", true)
