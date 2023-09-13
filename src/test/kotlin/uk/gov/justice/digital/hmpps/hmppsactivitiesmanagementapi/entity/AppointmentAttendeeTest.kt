package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendeeModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity

class AppointmentAttendeeTest {
  @Test
  fun `entity to model mapping`() {
    val expectedModel = appointmentAttendeeModel()
    assertThat(appointmentSeriesEntity().appointments().first().attendees().first().toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val expectedModel = listOf(appointmentAttendeeModel())
    assertThat(appointmentSeriesEntity().appointments().first().attendees().toModel()).isEqualTo(expectedModel)
  }
}
