package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier
import java.time.LocalDateTime

@ActiveProfiles("test")
@DataJpaTest
class ActivityRepositoryTest(
  @Autowired val repository: ActivityRepository,
  @Autowired val entityManager: TestEntityManager
) {
  private val timestamp = LocalDateTime.now()

  @Test
  fun `can create activity`() {
    val category = entityManager.persist(ActivityCategory(code = "code", description = "description"))
    val tier = entityManager.persist(ActivityTier(code = "T1", description = "Tier 1"))

    val activity = Activity(
      prisonCode = "123",
      activityCategory = category,
      activityTier = tier,
      summary = "Maths",
      description = "Maths basic",
      startDate = timestamp.toLocalDate(),
      createdTime = timestamp,
      createdBy = "me",
    ).also {
      assertThat(it.activityId).isNull()
    }

    val persisted = repository.save(activity)
    val activityPayRow = entityManager.persist(
      ActivityPay(activity = activity, incentiveLevel = "BAS", payBand = "A", rate = 100, pieceRate = 10, pieceRateItems = 1)
    )
    persisted.activityPay = mutableListOf(activityPayRow)

    with(persisted) {
      assertThat(activityId).isNotNull
      assertThat(prisonCode).isEqualTo("123")
      assertThat(tier).isEqualTo(tier)
      assertThat(category).isEqualTo(category)
      assertThat(summary).isEqualTo("Maths")
      assertThat(description).isEqualTo("Maths basic")
      assertThat(startDate).isEqualTo(timestamp.toLocalDate())
      assertThat(createdTime).isEqualTo(timestamp)
      assertThat(createdBy).isEqualTo("me")
      assertThat(eligibilityRules).isEmpty()
      assertThat(waitingList).isEmpty()
      assertThat(schedules).isEmpty()
      assertThat(activityPay[0].incentiveLevel).isEqualTo("BAS")
      assertThat(activityPay[0].payBand).isEqualTo("A")
      assertThat(activityPay[0].rate).isEqualTo(100)
      assertThat(endDate).isNull()
    }
  }
}
