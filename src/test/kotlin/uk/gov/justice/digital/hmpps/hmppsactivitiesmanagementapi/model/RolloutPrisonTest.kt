package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RolloutPrisonTest : ModelTest() {

  @Test
  fun `dates are serialized correctly`() {
    val originalRolloutDate = LocalDate.parse("01 Feb 2023", dateFormatter)

    val expectedRolloutDate = "2023-02-01"

    val rolloutPrison = RolloutPrisonPlan(
      prisonCode = "MDI",
      activitiesRolledOut = true,
      activitiesRolloutDate = originalRolloutDate,
      appointmentsRolledOut = true,
      appointmentsRolloutDate = originalRolloutDate,
    )

    val json = objectMapper.writeValueAsString(rolloutPrison)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["activitiesRolloutDate"]).isEqualTo(expectedRolloutDate)
    assertThat(jsonMap["appointmentsRolloutDate"]).isEqualTo(expectedRolloutDate)
  }
}
