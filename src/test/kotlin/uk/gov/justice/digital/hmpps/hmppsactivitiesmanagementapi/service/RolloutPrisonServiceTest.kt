package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrison as ModelRolloutPrison

class RolloutPrisonServiceTest {
  private val repository: RolloutPrisonRepository = mock()
  private val service = RolloutPrisonService(repository)

  @Test
  fun `returns an rollout prison for known prison code`() {
    whenever(repository.findByCode("PVI")).thenReturn(rolloutPrison())

    assertThat(service.getByPrisonCode("PVI")).isInstanceOf(ModelRolloutPrison::class.java)
  }

  @Test
  fun `throws entity not found exception for unknown prison code`() {
    assertThatThrownBy { service.getByPrisonCode("PVX") }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("PVX")
  }
}
