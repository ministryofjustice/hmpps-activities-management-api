package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RolloutPrisonPlanTest : ModelTest() {

  @Test
  fun `dates are serialized correctly`() {
    val rolloutPrison = RolloutPrisonPlan(
      prisonCode = "MDI",
      activitiesRolledOut = true,
      appointmentsRolledOut = true,
      prisonLive = true,
    )

    val json = objectMapper.writeValueAsString(rolloutPrison)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["prisonCode"]).isEqualTo("MDI")
    assertThat(jsonMap["activitiesRolledOut"]).isEqualTo(true)
    assertThat(jsonMap["appointmentsRolledOut"]).isEqualTo(true)
    assertThat(jsonMap["prisonLive"]).isEqualTo(true)
  }
}
