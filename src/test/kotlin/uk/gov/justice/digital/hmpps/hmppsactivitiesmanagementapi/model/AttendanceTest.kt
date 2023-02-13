package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AttendanceTest {

  private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm:ss")

  companion object {
    private val objectMapper = ObjectMapper()

    @JvmStatic
    @BeforeAll
    fun `setup`() {

      objectMapper.registerModule(JavaTimeModule())
      objectMapper.registerKotlinModule()
      objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
  }

  @Test
  fun `dates and times are serialized correctly`() {

    val originalRecordedTime = LocalDateTime.parse("31 Jan 2023 10:21:22", dateTimeFormatter)

    val expectedRecordedTime = "2023-01-31T10:21:22"

    val attendance = Attendance(
      id = 1,
      prisonerNumber = "1234",
      recordedTime = originalRecordedTime,
      posted = true,
      status = "Y"

    )

    val json = objectMapper.writeValueAsString(attendance)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    Assertions.assertThat(jsonMap["recordedTime"]).isEqualTo(expectedRecordedTime)
  }
}
