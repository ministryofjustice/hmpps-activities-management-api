package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
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
    val category = entityManager.persist(ActivityCategory(categoryCode = "code", description = "description"))
    val tier = entityManager.persist(ActivityTier(1, description = "Tier 1"))

    val activity = Activity(
      prisonCode = "123",
      activityCategory = category,
      activityTier = tier,
      summary = "Maths",
      description = "Maths basic",
      startDate = timestamp.toLocalDate(),
      createdAt = timestamp,
      createdBy = "me"
    ).also {
      assertThat(it.activityId).isNull()
    }

    val persisted = repository.save(activity)

    with(persisted) {
      assertThat(activityId).isNotNull
      assertThat(prisonCode).isEqualTo("123")
      assertThat(tier).isEqualTo(tier)
      assertThat(category).isEqualTo(category)
      assertThat(summary).isEqualTo("Maths")
      assertThat(description).isEqualTo("Maths basic")
      assertThat(startDate).isEqualTo(timestamp.toLocalDate())
      assertThat(createdAt).isEqualTo(timestamp)
      assertThat(createdBy).isEqualTo("me")
      assertThat(eligibilityRules).isEmpty()
      assertThat(sessions).isEmpty()
      assertThat(activityPay).isNull()
      assertThat(active).isFalse
      assertThat(endDate).isNull()
    }
  }
}
