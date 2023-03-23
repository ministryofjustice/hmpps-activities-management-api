package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryEntities

class AppointmentCategoryServiceTest {
  private val appointmentCategoryRepository: AppointmentCategoryRepository = mock()

  private val service = AppointmentCategoryService(appointmentCategoryRepository)

  @Test
  fun `findAll returns all appointment categories`() {
    val entities = appointmentCategoryEntities()
    whenever(appointmentCategoryRepository.findAllOrdered()).thenReturn(entities)
    assertThat(service.getAll(false)).isEqualTo(entities.toModel())
  }
}
