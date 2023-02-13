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
import java.time.format.DateTimeFormatter

class AllocationTest {

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
    val originalAllocatedTime = LocalDateTime.parse("31 Jan 2023 10:21:22", dateTimeFormatter)
    val originalDeallocatedTime = LocalDateTime.parse("31 Jan 2023 12:13:14", dateTimeFormatter)

    val expectedStartDate = "2023-02-01"
    val expectedEndDate = "2023-02-07"
    val expectedAllocatedTime = "2023-01-31T10:21:22"
    val expectedDeallocatedTime = "2023-01-31T12:13:14"

    val allocation = Allocation(
      id = 1,
      startDate = originalStartDate,
      endDate = originalEndDate,
      allocatedTime = originalAllocatedTime,
      deallocatedTime = originalDeallocatedTime,
      activitySummary = "Blah",
      bookingId = 123,
      prisonPayBand = PrisonPayBand(1, 1, "Alias", "Desc", 1, "PVI"),
      prisonerNumber = "1234",
      scheduleDescription = "Blah blah"

    )

    val json = objectMapper.writeValueAsString(allocation)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    Assertions.assertThat(jsonMap["startDate"]).isEqualTo(expectedStartDate)
    Assertions.assertThat(jsonMap["endDate"]).isEqualTo(expectedEndDate)
    Assertions.assertThat(jsonMap["allocatedTime"]).isEqualTo(expectedAllocatedTime)
    Assertions.assertThat(jsonMap["deallocatedTime"]).isEqualTo(expectedDeallocatedTime)
  }
}
