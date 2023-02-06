package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceAllocation

class AppointmentOccurrenceAllocationTest {
  @Test
  fun `entity to model mapping`() {
    val expectedModel = appointmentOccurrenceAllocationModel()
    Assertions.assertThat(appointmentEntity().occurrences()[0].allocations()[0].toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val expectedModel = listOf(appointmentOccurrenceAllocationModel())
    Assertions.assertThat(appointmentEntity().occurrences()[0].allocations().toModel()).isEqualTo(expectedModel)
  }
}

internal fun appointmentOccurrenceAllocationModel() =
  AppointmentOccurrenceAllocation(1, "A1234BC", 456)
