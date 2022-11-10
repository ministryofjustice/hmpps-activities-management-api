package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrison
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityEligibility as ModelActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier as ModelActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EligibilityRule as ModelEligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerWaiting as ModelActivityWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstance as ModelScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

class TransformFunctionsTest {

  @Test
  fun `transformation of activity entity to the activity models`() {
    val timestamp = LocalDateTime.now()

    with(transform(activityEntity(timestamp = timestamp))) {
      assertThat(id).isEqualTo(1)
      assertThat(prisonCode).isEqualTo("123")
      assertThat(summary).isEqualTo("Maths")
      assertThat(description).isEqualTo("Maths basic")
      assertThat(category).isEqualTo(ModelActivityCategory(id = 1, description = "category description"))
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
            ModelScheduledInstance(
              id = 1,
              date = timestamp.toLocalDate(),
              startTime = timestamp.toLocalTime(),
              endTime = timestamp.toLocalTime(),
              cancelled = false,
              attendances = listOf(
                ModelAttendance(
                  id = 1,
                  prisonerNumber = "A11111A",
                  posted = false
                )
              )
            )
          ),
          internalLocation = InternalLocation(1, "EDU-ROOM-1", "Education - R1"),
          allocations = listOf(
            Allocation(
              id = 1,
              prisonerNumber = "A1234AA",
              incentiveLevel = "BAS",
              payBand = "A",
              startDate = timestamp.toLocalDate(),
              endDate = null,
              allocatedTime = timestamp,
              allocatedBy = "Mr Blogs",
            )
          ),
          description = "schedule description",
          startTime = timestamp.toLocalTime(),
          endTime = timestamp.toLocalTime(),
          capacity = 1,
          daysOfWeek = listOf("Mon")
        )
      )
      assertThat(waitingList).containsExactly(
        ModelActivityWaiting(id = 1, prisonerNumber = "A1234AA", priority = 1, createdTime = timestamp, createdBy = "test")
      )
      assertThat(pay).containsExactly(
        ModelActivityPay(id = 1, incentiveLevel = "BAS", payBand = "A", rate = 30, pieceRate = 40, pieceRateItems = 50)
      )
      assertThat(startDate).isEqualTo(timestamp.toLocalDate())
      assertThat(endDate).isNull()
      assertThat(createdTime).isEqualTo(timestamp)
      assertThat(createdBy).isEqualTo("test")
    }
  }

  @Test
  fun `transformation of rollout prison entity to rollout prison model`() {
    assertThat(transform(rolloutPrison())).isEqualTo(RolloutPrison(1, "PVI", "HMP Pentonville", true))
  }
}
