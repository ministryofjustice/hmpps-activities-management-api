package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentInstanceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentInstanceModel

class AppointmentInstanceTest {
  @Test
  fun `entity to model mapping`() {
    val entity = appointmentInstanceEntity()
    val expectedModel = appointmentInstanceModel(entity.createdTime, entity.updatedTime)
    assertThat(entity.toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val entity = appointmentInstanceEntity()
    val expectedModel = listOf(appointmentInstanceModel(entity.createdTime, entity.updatedTime))
    assertThat(listOf(entity).toModel()).isEqualTo(expectedModel)
  }
}
