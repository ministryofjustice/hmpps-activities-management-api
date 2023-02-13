package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SuspensionTest {

  private val formatter = DateTimeFormatter.ofPattern("dd MMM uuuu")

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

    val originalSuspendedFrom = LocalDate.parse("01 Feb 2023", formatter)
    val originalSuspendedUntil = LocalDate.parse("07 Feb 2023", formatter)
    val expectedSuspendedFrom = "2023-02-01"
    val expectedSuspendedUntil = "2023-02-07"

    val suspension = Suspension(suspendedFrom = originalSuspendedFrom, suspendedUntil = originalSuspendedUntil)

    val json = objectMapper.writeValueAsString(suspension)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["suspendedFrom"]).isEqualTo(expectedSuspendedFrom)
    assertThat(jsonMap["suspendedUntil"]).isEqualTo(expectedSuspendedUntil)
  }
}
