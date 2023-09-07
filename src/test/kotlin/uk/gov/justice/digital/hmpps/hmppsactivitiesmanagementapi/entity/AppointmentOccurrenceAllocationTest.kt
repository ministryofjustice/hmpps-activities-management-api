package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceAllocationModel

class AppointmentOccurrenceAllocationTest {
  @Test
  fun `entity to model mapping`() {
    val expectedModel = appointmentOccurrenceAllocationModel()
    assertThat(appointmentSeriesEntity().appointments().first().allocations().first().toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val expectedModel = listOf(appointmentOccurrenceAllocationModel())
    assertThat(appointmentSeriesEntity().appointments().first().allocations().toModel()).isEqualTo(expectedModel)
  }
}
