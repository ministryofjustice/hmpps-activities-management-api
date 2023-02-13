package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ActivityTest {

  private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM uuuu")
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

    val originalStartDate = LocalDate.parse("01 Feb 2023", dateFormatter)
    val originalEndDate = LocalDate.parse("07 Feb 2023", dateFormatter)
    val originalCreatedTime = LocalDateTime.parse("31 Jan 2023 10:21:22", dateTimeFormatter)

    val expectedStartDate = "2023-02-01"
    val expectedEndDate = "2023-02-07"
    val expectedCreatedTime = "2023-01-31T10:21:22"

    val activity = Activity(
      id = 1,
      inCell = false,
      minimumIncentiveLevel = "Some incentive level",
      outsideWork = true,
      pieceWork = false,
      prisonCode = "PVI",
      riskLevel = "Low",
      tier = ActivityTier(id = 2, code = "AB", description = "Activity desc"),
      attendanceRequired = true,
      category = ActivityCategory(id = 1, code = "11", name = "Cat 1", description = "Cat 1 desc"),
      description = "Some Desc",
      summary = "Blah",
      startDate = originalStartDate,
      endDate = originalEndDate,
      createdBy = "TestUser",
      createdTime = originalCreatedTime
    )

    val json = objectMapper.writeValueAsString(activity)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    Assertions.assertThat(jsonMap["startDate"]).isEqualTo(expectedStartDate)
    Assertions.assertThat(jsonMap["endDate"]).isEqualTo(expectedEndDate)
    Assertions.assertThat(jsonMap["createdTime"]).isEqualTo(expectedCreatedTime)
  }
}
