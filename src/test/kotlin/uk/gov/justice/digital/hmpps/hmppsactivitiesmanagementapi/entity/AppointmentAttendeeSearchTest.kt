package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendeeSearchResultModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity

class AppointmentAttendeeSearchTest {
  @Test
  fun `entity to model mapping`() {
    val expectedModel = appointmentAttendeeSearchResultModel()
    assertThat(appointmentSearchEntity().attendees.first().toResult()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val expectedModel = listOf(appointmentAttendeeSearchResultModel())
    assertThat(appointmentSearchEntity().attendees.toResult()).isEqualTo(expectedModel)
  }
}
