package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendeeModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity

class AppointmentAttendeeSearchTest {
  @Test
  fun `entity to model mapping`() {
    val expectedModel = appointmentAttendeeModel()
    assertThat(appointmentSearchEntity().attendees.first().toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val expectedModel = listOf(appointmentAttendeeModel())
    assertThat(appointmentSearchEntity().attendees.toModel()).isEqualTo(expectedModel)
  }
}
