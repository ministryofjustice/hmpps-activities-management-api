package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService

class RolloutPrisonServiceTest {
  private val repository: RolloutPrisonRepository = mock()
  private val service = RolloutPrisonService(repository)

  @Test
  fun `returns an rollout prison plan for known prison code`() {
    whenever(repository.findByCode("PVI")).thenReturn(rolloutPrison())

    assertThat(service.getByPrisonCode("PVI")).isInstanceOf(RolloutPrisonPlan::class.java)
  }

  @Test
  fun `returns a default rollout prison plan for an unknown prison code`() {
    whenever(repository.findByCode("PVX")).thenReturn(null)

    assertThat(service.getByPrisonCode("PVX"))
      .isInstanceOf(RolloutPrisonPlan::class.java)
      .hasFieldOrPropertyWithValue("prisonCode", "PVX")
      .hasFieldOrPropertyWithValue("activitiesRolledOut", false)
      .hasFieldOrPropertyWithValue("appointmentsRolledOut", false)
  }
}
