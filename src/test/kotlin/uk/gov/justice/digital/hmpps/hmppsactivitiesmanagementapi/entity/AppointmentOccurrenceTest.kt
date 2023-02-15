package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceModel

class AppointmentOccurrenceTest {
  @Test
  fun `entity to model mapping`() {
    val entity = appointmentEntity().occurrences().first()
    val expectedModel = appointmentOccurrenceModel(entity.updated)
    assertThat(entity.toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val entityList = appointmentEntity().occurrences()
    val expectedModel = listOf(appointmentOccurrenceModel(entityList.first().updated))
    assertThat(entityList.toModel()).isEqualTo(expectedModel)
  }
}
