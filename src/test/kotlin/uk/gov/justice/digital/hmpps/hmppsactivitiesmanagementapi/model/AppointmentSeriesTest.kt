package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventTier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AppointmentSeriesTest : ModelTest() {
  @Test
  fun `dates and times are serialized correctly`() {
    val originalStartDate = LocalDate.parse("01 Feb 2023", dateFormatter)
    val originalStartTime = LocalTime.parse("10:21:22", timeFormatter)
    val originalEndTime = LocalTime.parse("11:22:23", timeFormatter)
    val originalCreatedTime = LocalDateTime.parse("31 Jan 2023 09:01:02", dateTimeFormatter)
    val originalUpdatedTime = LocalDateTime.parse("01 Feb 2023 10:02:03", dateTimeFormatter)

    val expectedStartDate = "2023-02-01"
    val expectedStartTime = "10:21"
    val expectedEndTime = "11:22"
    val expectedCreatedTime = "2023-01-31T09:01:02"
    val expectedUpdatedTime = "2023-02-01T10:02:03"

    val appointmentSeries = AppointmentSeries(
      id = 1,
      appointmentType = AppointmentType.INDIVIDUAL,
      prisonCode = "PVI",
      categoryCode = "C11",
      tier = eventTier().toModelEventTier(),
      organiser = eventOrganiser().toModelEventOrganiser(),
      customName = "Appointment description",
      internalLocationId = null,
      inCell = true,
      startDate = originalStartDate,
      startTime = originalStartTime,
      endTime = originalEndTime,
      schedule = null,
      extraInformation = "Blah",
      createdTime = originalCreatedTime,
      createdBy = "A. Jones",
      updatedTime = originalUpdatedTime,
      updatedBy = "A.Jones",
    )

    val json = objectMapper.writeValueAsString(appointmentSeries)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["startDate"]).isEqualTo(expectedStartDate)
    assertThat(jsonMap["startTime"]).isEqualTo(expectedStartTime)
    assertThat(jsonMap["endTime"]).isEqualTo(expectedEndTime)
    assertThat(jsonMap["createdTime"]).isEqualTo(expectedCreatedTime)
    assertThat(jsonMap["updatedTime"]).isEqualTo(expectedUpdatedTime)
  }
}
