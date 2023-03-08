package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategorySummary

class AppointmentCategoryTest {
  @Test
  fun `entity to model mapping`() {
    val expectedModel = appointmentCategoryModel()
    assertThat(appointmentCategoryEntity().toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val expectedModel = listOf(appointmentCategoryModel())
    assertThat(listOf(appointmentCategoryEntity().toModel())).isEqualTo(expectedModel)
  }

  @Test
  fun `entity to summary mapping`() {
    val expectedSummary = appointmentCategorySummary()
    assertThat(appointmentCategoryEntity().toSummary()).isEqualTo(expectedSummary)
  }
}
