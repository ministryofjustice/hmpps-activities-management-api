package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

class ActivityCategoryTest {
  @Test
  fun `converted to model`() {
    val expectedModel = ModelActivityCategory(1, "category code", "category description")
    assertThat(activityCategory().toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `List converted to model`() {
    val expectedModel = listOf(ModelActivityCategory(1, "category code", "category description"))
    assertThat(listOf(activityCategory()).toModel()).isEqualTo(expectedModel)
  }
}
