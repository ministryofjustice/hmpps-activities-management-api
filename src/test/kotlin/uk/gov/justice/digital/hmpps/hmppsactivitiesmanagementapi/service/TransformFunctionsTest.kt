package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityCategory as ModelActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityEligibility as ModelActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityInstance as ModelActivityInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPayBand as ModelActivityPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySession as ModelActivitySession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier as ModelActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityWaiting as ModelActivityWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EligibilityRule as ModelEligibilityRule

class TransformFunctionsTest {

  @Test
  fun `transformation of activity entity to the activity models`() {
    val timestamp = LocalDateTime.now()

    with(transform(activityEntity(timestamp = timestamp))) {
      assertThat(id).isEqualTo(1)
      assertThat(prisonCode).isEqualTo("123")
      assertThat(summary).isEqualTo("Maths")
      assertThat(description).isEqualTo("Maths basic")
      assertThat(category).isEqualTo(ModelActivityCategory(id = 1, code = "code", description = "category description"))
      assertThat(tier).isEqualTo(ModelActivityTier(1, "T1", "Tier 1"))
      assertThat(eligibilityRules).containsExactly(
        ModelActivityEligibility(
          1,
          ModelEligibilityRule(1, code = "code", description = "rule description")
        )
      )
      assertThat(sessions).containsExactly(
        ModelActivitySession(
          id = 1,
          instances = listOf(
            ModelActivityInstance(
              id = 1,
              sessionDate = timestamp.toLocalDate(),
              startTime = timestamp,
              endTime = timestamp,
              cancelled = false
            )
          ),
          prisoners = emptyList(), // TODO specify prisoners
          description = "session description",
          startTime = timestamp,
          endTime = timestamp,
          capacity = 1,
          daysOfWeek = "0000001"
        )
      )
      assertThat(waitingList).containsExactly(
        ModelActivityWaiting(
          id = 1,
          prisonerNumber = "1234567890",
          priority = 1,
          createdTime = timestamp,
          createdBy = "test"
        )
      )
      assertThat(pay).isEqualTo(
        ModelActivityPay(
          id = 1,
          bands = listOf(
            ModelActivityPayBand(
              id = 1
            )
          )
        )
      )
      assertThat(startDate).isEqualTo(timestamp.toLocalDate())
      assertThat(endDate).isNull()
      assertThat(active).isTrue
      assertThat(createdTime).isEqualTo(timestamp)
      assertThat(createdBy).isEqualTo("test")
    }
  }
}

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
