package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AppointmentOccurrenceTest : ModelTest() {
  @Test
  fun `dates and times are serialized correctly`() {
    val originalStartDate = LocalDate.parse("01 Feb 2023", dateFormatter)
    val originalStartTime = LocalTime.parse("10:21:22", timeFormatter)
    val originalEndTime = LocalTime.parse("11:22:23", timeFormatter)
    val originalUpdatedTime = LocalDateTime.parse("01 Feb 2023 10:02:03", dateTimeFormatter)

    val expectedStartDate = "2023-02-01"
    val expectedStartTime = "10:21"
    val expectedEndTime = "11:22"
    val expectedUpdatedTime = "2023-02-01T10:02:03"

    val appointmentOccurrence = AppointmentOccurrence(
      id = 1,
      sequenceNumber = 1,
      internalLocationId = null,
      comment = "Blah",
      startDate = originalStartDate,
      startTime = originalStartTime,
      endTime = originalEndTime,
      inCell = true,
      updatedBy = "A.Jones",
      updated = originalUpdatedTime,
    )

    val json = objectMapper.writeValueAsString(appointmentOccurrence)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["startDate"]).isEqualTo(expectedStartDate)
    assertThat(jsonMap["startTime"]).isEqualTo(expectedStartTime)
    assertThat(jsonMap["endTime"]).isEqualTo(expectedEndTime)
    assertThat(jsonMap["updated"]).isEqualTo(expectedUpdatedTime)
  }
}
