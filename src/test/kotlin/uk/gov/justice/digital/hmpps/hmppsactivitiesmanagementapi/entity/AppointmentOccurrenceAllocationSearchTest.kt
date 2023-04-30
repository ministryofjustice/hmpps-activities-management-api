package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceAllocationModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceSearchEntity

class AppointmentOccurrenceAllocationSearchTest {
  @Test
  fun `entity to model mapping`() {
    val expectedModel = appointmentOccurrenceAllocationModel()
    assertThat(appointmentOccurrenceSearchEntity().allocations.first().toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val expectedModel = listOf(appointmentOccurrenceAllocationModel())
    assertThat(appointmentOccurrenceSearchEntity().allocations.toModel()).isEqualTo(expectedModel)
  }
}
