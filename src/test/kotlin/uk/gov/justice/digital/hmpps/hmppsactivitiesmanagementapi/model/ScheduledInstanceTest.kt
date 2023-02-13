package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduledInstanceTest : ModelTest() {

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

    val scheduledInstance = ScheduledInstance(
      id = 1,
      date = originalDate,
      startTime = originalStartTime,
      endTime = originalEndTime,
      cancelled = true,
      cancelledTime = originalCancelledTime,
      attendances = emptyList(),
    )

    val json = objectMapper.writeValueAsString(scheduledInstance)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["date"]).isEqualTo(expectedDate)
    assertThat(jsonMap["startTime"]).isEqualTo(expectedStartTime)
    assertThat(jsonMap["endTime"]).isEqualTo(expectedEndTime)
    assertThat(jsonMap["cancelledTime"]).isEqualTo(expectedCancelledTime)
  }
}
