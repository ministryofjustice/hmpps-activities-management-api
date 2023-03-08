package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentInstanceModel

class AppointmentInstanceTest {
  @Test
  fun `entity to model mapping`() {
    val appointmentEntity = appointmentEntity()
    val entity = appointmentEntity.occurrences().first().instances().first()
    val expectedModel = appointmentInstanceModel()
    assertThat(entity.toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val appointmentEntity = appointmentEntity()
    val entity = appointmentEntity.occurrences().first().instances().first()
    val expectedModel = listOf(appointmentInstanceModel())
    assertThat(listOf(entity).toModel()).isEqualTo(expectedModel)
  }
}
