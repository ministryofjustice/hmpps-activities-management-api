package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentCategory

class AppointmentCategoryTest {
  @Test
  fun `entity to model mapping`() {
    val expectedModel = AppointmentCategory(1, null, "TEST", "Test Category", true, 2)
    Assertions.assertThat(appointmentCategoryEntity().toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val expectedModel = listOf(AppointmentCategory(1, null, "TEST", "Test Category", true, 2))
    Assertions.assertThat(listOf(appointmentCategoryEntity().toModel())).isEqualTo(expectedModel)
  }
}