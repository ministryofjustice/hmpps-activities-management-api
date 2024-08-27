package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService

class RolloutPrisonServiceTest {
  private val service = RolloutPrisonService("PVI", "PVI")

  @Test
  fun `returns an rollout prison plan for known prison code`() {
    assertThat(service.getByPrisonCode("PVI")).isInstanceOf(RolloutPrisonPlan::class.java)
  }

  @Test
  fun `returns a default rollout prison plan for an unknown prison code`() {
    assertThat(service.getByPrisonCode("PVX"))
      .isInstanceOf(RolloutPrisonPlan::class.java)
      .hasFieldOrPropertyWithValue("prisonCode", "PVX")
      .hasFieldOrPropertyWithValue("activitiesRolledOut", false)
      .hasFieldOrPropertyWithValue("appointmentsRolledOut", false)
  }
}
