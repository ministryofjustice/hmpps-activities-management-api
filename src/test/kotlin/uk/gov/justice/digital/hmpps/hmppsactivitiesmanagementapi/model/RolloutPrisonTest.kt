package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RolloutPrisonTest {

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

    val originalRolloutDate = LocalDate.parse("01 Feb 2023", dateFormatter)

    val expectedRolloutDate = "2023-02-01"

    val rolloutPrison = RolloutPrison(
      id = 1,
      description = "Some Desc",
      rolloutDate = originalRolloutDate,
      active = true,
      code = "1234"
    )

    val json = objectMapper.writeValueAsString(rolloutPrison)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    Assertions.assertThat(jsonMap["rolloutDate"]).isEqualTo(expectedRolloutDate)
  }
}
