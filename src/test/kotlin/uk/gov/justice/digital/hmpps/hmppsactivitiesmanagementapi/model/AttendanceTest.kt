package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AttendanceTest : ModelTest() {

  @Test
  fun `dates and times are serialized correctly`() {
    val originalRecordedTime = LocalDateTime.parse("31 Jan 2023 10:21:22", dateTimeFormatter)

    val expectedRecordedTime = "2023-01-31T10:21:22"

    val attendance = Attendance(
      id = 1,
      prisonerNumber = "1234",
      recordedTime = originalRecordedTime,
      status = "Y",

    )

    val json = objectMapper.writeValueAsString(attendance)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["recordedTime"]).isEqualTo(expectedRecordedTime)
  }
}
