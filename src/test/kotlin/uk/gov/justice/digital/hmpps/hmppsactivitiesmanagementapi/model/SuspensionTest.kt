package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SuspensionTest : ModelTest() {

  @Test
  fun `dates are serialized correctly`() {

    val originalSuspendedFrom = LocalDate.parse("01 Feb 2023", dateFormatter)
    val originalSuspendedUntil = LocalDate.parse("07 Feb 2023", dateFormatter)
    val expectedSuspendedFrom = "2023-02-01"
    val expectedSuspendedUntil = "2023-02-07"

    val suspension = Suspension(suspendedFrom = originalSuspendedFrom, suspendedUntil = originalSuspendedUntil)

    val json = objectMapper.writeValueAsString(suspension)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["suspendedFrom"]).isEqualTo(expectedSuspendedFrom)
    assertThat(jsonMap["suspendedUntil"]).isEqualTo(expectedSuspendedUntil)
  }
}
