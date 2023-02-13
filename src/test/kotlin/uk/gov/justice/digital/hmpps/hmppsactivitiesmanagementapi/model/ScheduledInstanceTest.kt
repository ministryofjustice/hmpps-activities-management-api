package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ScheduledInstanceTest {

  private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM uuuu")
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm:ss")
  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

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

    Assertions.assertThat(jsonMap["date"]).isEqualTo(expectedDate)
    Assertions.assertThat(jsonMap["startTime"]).isEqualTo(expectedStartTime)
    Assertions.assertThat(jsonMap["endTime"]).isEqualTo(expectedEndTime)
    Assertions.assertThat(jsonMap["cancelledTime"]).isEqualTo(expectedCancelledTime)
  }
}
