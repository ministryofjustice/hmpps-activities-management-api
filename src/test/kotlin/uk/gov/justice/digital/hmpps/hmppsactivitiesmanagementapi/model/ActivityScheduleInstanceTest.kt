package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ActivityScheduleInstanceTest : ModelTest() {

  @Test
  fun `dates and times are serialized correctly`() {

    val originalDate = LocalDate.parse("01 Feb 2023", dateFormatter)
    val originalStartTime = LocalTime.parse("10:21:22", timeFormatter)
    val originalEndTime = LocalTime.parse("11:22:23", timeFormatter)
    val originalCancelledTime = LocalDateTime.parse("31 Jan 2023 09:01:02", dateTimeFormatter)

    val expectedDate = "2023-02-01"
    val expectedStartTime = "10:21"
    val expectedEndTime = "11:22"
    val expectedCancelledTime = "2023-01-31T09:01:02"

    val activityScheduleInstance = ActivityScheduleInstance(
      id = 1,
      date = originalDate,
      startTime = originalStartTime,
      endTime = originalEndTime,
      cancelled = true,
      cancelledTime = originalCancelledTime,
      attendances = emptyList(),
      activitySchedule = ActivityScheduleLite(
        id = 1,
        activity = ActivityLite(
          id = 1,
          inCell = false,
          minimumIncentiveLevel = "Some incentive level",
          outsideWork = true,
          pieceWork = false,
          prisonCode = "PVI",
          riskLevel = "Low",
          attendanceRequired = true,
          category = ActivityCategory(id = 1, code = "11", name = "Cat 1", description = "Cat 1 desc"),
          description = "Some Desc",
          summary = "Blah"
        ),
        description = "Some Desc",
        capacity = 10,
        startDate = LocalDate.now(),
        endDate = LocalDate.now()
      )
    )

    val json = objectMapper.writeValueAsString(activityScheduleInstance)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["date"]).isEqualTo(expectedDate)
    assertThat(jsonMap["startTime"]).isEqualTo(expectedStartTime)
    assertThat(jsonMap["endTime"]).isEqualTo(expectedEndTime)
    assertThat(jsonMap["cancelledTime"]).isEqualTo(expectedCancelledTime)
  }
}
