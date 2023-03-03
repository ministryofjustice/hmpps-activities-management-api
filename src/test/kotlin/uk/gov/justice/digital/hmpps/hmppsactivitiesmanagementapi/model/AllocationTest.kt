package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AllocationTest : ModelTest() {

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
      scheduleDescription = "Blah blah",
      scheduleId = 123,
    )

    val json = objectMapper.writeValueAsString(allocation)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["startDate"]).isEqualTo(expectedStartDate)
    assertThat(jsonMap["endDate"]).isEqualTo(expectedEndDate)
    assertThat(jsonMap["allocatedTime"]).isEqualTo(expectedAllocatedTime)
    assertThat(jsonMap["deallocatedTime"]).isEqualTo(expectedDeallocatedTime)
  }
}
