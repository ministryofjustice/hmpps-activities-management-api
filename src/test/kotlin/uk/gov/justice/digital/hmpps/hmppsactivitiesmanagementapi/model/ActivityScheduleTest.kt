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
import java.time.format.DateTimeFormatter

class ActivityScheduleTest {

  private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM uuuu")

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
      startDate = originalStartDate,
      endDate = originalEndDate
    )

    val json = objectMapper.writeValueAsString(activitySchedule)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    Assertions.assertThat(jsonMap["startDate"]).isEqualTo(expectedStartDate)
    Assertions.assertThat(jsonMap["endDate"]).isEqualTo(expectedEndDate)
  }
}
