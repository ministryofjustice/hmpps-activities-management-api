package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RolloutPrisonTest : ModelTest() {

  @Test
  fun `dates are serialized correctly`() {
    val originalRolloutDate = LocalDate.parse("01 Feb 2023", dateFormatter)

    val expectedRolloutDate = "2023-02-01"

    val rolloutPrison = RolloutPrison(
      id = 1,
      description = "Some Desc",
      rolloutDate = originalRolloutDate,
      active = true,
      code = "1234",
      isAppointmentsEnabled = true,
    )

    val json = objectMapper.writeValueAsString(rolloutPrison)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["rolloutDate"]).isEqualTo(expectedRolloutDate)
  }
}
