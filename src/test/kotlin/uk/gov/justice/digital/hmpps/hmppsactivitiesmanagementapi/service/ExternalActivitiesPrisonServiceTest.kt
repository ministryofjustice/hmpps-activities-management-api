package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ExternalActivitiesPrison
import java.util.Properties
import kotlin.apply

class ExternalActivitiesPrisonServiceTest {

  private lateinit var properties: Properties
  private lateinit var externalActivitiesPrisonService: ExternalActivitiesPrisonService

  @BeforeEach
  fun setUp() {
    properties = Properties().apply {
      put("FDI", "Ford")
      put("HDI", "Hatfield")
      put("GNI", "Grendon")
    }
    externalActivitiesPrisonService = ExternalActivitiesPrisonService(properties)
  }

  @Test
  fun `should return a list of prisons sorted by prison code`() {
    val result = externalActivitiesPrisonService.getPrisonsEnabledForExternalActivities()
    val expected = listOf(
      ExternalActivitiesPrison("FDI", "Ford"),
      ExternalActivitiesPrison("GNI", "Grendon"),
      ExternalActivitiesPrison("HDI", "Hatfield"),
    )
    assertEquals(expected, result)
  }

  @Test
  fun `should return an empty list when prisons are not configured`() {
    val serviceWithEmptyProperties = ExternalActivitiesPrisonService(Properties())
    val result = serviceWithEmptyProperties.getPrisonsEnabledForExternalActivities()
    assertThat(result).isEmpty()
  }
}
