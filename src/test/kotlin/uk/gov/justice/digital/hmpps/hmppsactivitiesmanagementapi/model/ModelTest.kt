package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.BeforeAll
import java.time.format.DateTimeFormatter

abstract class ModelTest {

  companion object {

    val objectMapper = ObjectMapper()
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM uuuu")
    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm:ss")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    @JvmStatic
    @BeforeAll
    fun `setup`() {
      objectMapper.registerModule(JavaTimeModule())
      objectMapper.registerKotlinModule()
      objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
  }
}
