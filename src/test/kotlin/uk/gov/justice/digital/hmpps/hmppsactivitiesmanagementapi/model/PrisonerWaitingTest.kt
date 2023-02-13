package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PrisonerWaitingTest : ModelTest() {
  @Test
  fun `dates and times are serialized correctly`() {

    val originalCreatedTime = LocalDateTime.parse("31 Jan 2023 10:21:22", dateTimeFormatter)

    val expectedCreatedTime = "2023-01-31T10:21:22"

    val prisonerWaiting = PrisonerWaiting(
      id = 1,
      createdBy = "TestUser",
      createdTime = originalCreatedTime,
      priority = 1,
      prisonerNumber = "1234"
    )

    val json = objectMapper.writeValueAsString(prisonerWaiting)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["createdTime"]).isEqualTo(expectedCreatedTime)
  }
}
