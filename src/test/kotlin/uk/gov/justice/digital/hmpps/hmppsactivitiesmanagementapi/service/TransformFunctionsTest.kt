package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPrisoner
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityCategory as ModelActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityEligibility as ModelActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityInstance as ModelActivityInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPayBand as ModelActivityPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule
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
      assertThat(schedules).containsExactly(
        ModelActivitySchedule(
          id = 1,
          instances = listOf(
            ModelActivityInstance(
              id = 1,
              date = timestamp.toLocalDate(),
              startTime = timestamp.toLocalTime(),
              endTime = timestamp.toLocalTime(),
              cancelled = false
            )
          ),
          prisoners = listOf(
            ActivityPrisoner(
              id = 1,
              prisonerNumber = "A1234AA",
              iepLevel = "BAS",
              payBand = "A",
              startDate = timestamp.toLocalDate(),
              endDate = null,
              active = true,
              allocatedTime = timestamp,
              allocatedBy = "Mr Blogs",
            )
          ),
          description = "schedule description",
          startTime = timestamp.toLocalTime(),
          endTime = timestamp.toLocalTime(),
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
          ),
          iepBasicRate = 10,
          iepStandardRate = 20,
          iepEnhancedRate = 30,
          pieceRate = 40,
          pieceRateItems = 50
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
