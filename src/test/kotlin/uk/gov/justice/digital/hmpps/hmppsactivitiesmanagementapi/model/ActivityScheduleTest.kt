package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import java.time.LocalDate
import java.time.LocalDateTime

class ActivityScheduleTest : ModelTest() {

  @Test
  fun `dates are serialized correctly`() {
    val originalStartDate = LocalDate.parse("01 Feb 2023", dateFormatter)
    val originalEndDate = LocalDate.parse("07 Feb 2023", dateFormatter)

    val expectedStartDate = "2023-02-01"
    val expectedEndDate = "2023-02-07"

    val activitySchedule = ActivitySchedule(
      id = 1,
      activity = ActivityLite(
        id = 1,
        inCell = false,
        onWing = false,
        offWing = false,
        outsideWork = true,
        pieceWork = false,
        prisonCode = "PVI",
        riskLevel = "Low",
        attendanceRequired = true,
        category = ActivityCategory(id = 1, code = "11", name = "Cat 1", description = "Cat 1 desc"),
        description = "Some Desc",
        summary = "Blah",
        capacity = 10,
        allocated = 5,
        createdTime = LocalDate.now().atStartOfDay(),
        activityState = ActivityState.LIVE,
        paid = true,
      ),
      description = "Some Desc",
      capacity = 10,
      startDate = originalStartDate,
      endDate = originalEndDate,
      runsOnBankHoliday = true,
      updatedTime = LocalDateTime.parse("31 Jan 2023 10:21:22", dateTimeFormatter),
      updatedBy = "TestUser",
      scheduleWeeks = 1,
    )

    val json = objectMapper.writeValueAsString(activitySchedule)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["startDate"]).isEqualTo(expectedStartDate)
    assertThat(jsonMap["endDate"]).isEqualTo(expectedEndDate)
  }
}
