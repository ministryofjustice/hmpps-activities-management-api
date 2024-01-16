package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import java.time.LocalDate
import java.time.LocalDateTime

class ActivityTest : ModelTest() {

  @Test
  fun `dates and times are serialized correctly`() {
    val originalStartDate = LocalDate.parse("01 Feb 2023", dateFormatter)
    val originalEndDate = LocalDate.parse("07 Feb 2023", dateFormatter)
    val originalCreatedTime = LocalDateTime.parse("31 Jan 2023 10:21:22", dateTimeFormatter)

    val expectedStartDate = "2023-02-01"
    val expectedEndDate = "2023-02-07"
    val expectedCreatedTime = "2023-01-31T10:21:22"

    val activity = Activity(
      id = 1,
      inCell = false,
      onWing = false,
      offWing = false,
      outsideWork = true,
      pieceWork = false,
      prisonCode = "PVI",
      riskLevel = "Low",
      tier = EventTier(id = 2, code = "AB", description = "Activity desc"),
      attendanceRequired = true,
      category = ActivityCategory(id = 1, code = "11", name = "Cat 1", description = "Cat 1 desc"),
      description = "Some Desc",
      summary = "Blah",
      startDate = originalStartDate,
      endDate = originalEndDate,
      createdBy = "TestUser",
      createdTime = originalCreatedTime,
      updatedBy = "TestUser",
      updatedTime = originalCreatedTime,
      paid = true,
    )

    val json = objectMapper.writeValueAsString(activity)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["startDate"]).isEqualTo(expectedStartDate)
    assertThat(jsonMap["endDate"]).isEqualTo(expectedEndDate)
    assertThat(jsonMap["createdTime"]).isEqualTo(expectedCreatedTime)
  }
}
