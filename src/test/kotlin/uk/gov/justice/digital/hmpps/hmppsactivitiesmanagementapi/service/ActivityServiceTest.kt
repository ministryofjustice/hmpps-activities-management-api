package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import java.util.Optional
import javax.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity

class ActivityServiceTest {
  private val repository: ActivityRepository = mock()
  private val service = ActivityService(repository)

  @Test
  fun `returns an activity for known activity ID`() {
    whenever(repository.findById(1)).thenReturn(Optional.of(activityEntity()))

    assertThat(service.getActivityById(1)).isInstanceOf(ModelActivity::class.java)
  }

  @Test
  fun `throws entity not found exception for unknown activity ID`() {
    whenever(repository.findById(-1)).thenReturn(Optional.empty())

    assertThatThrownBy { service.getActivityById(-1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .withFailMessage { "-1" }
  }
}
