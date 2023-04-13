package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentInstanceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import java.util.Optional

class AppointmentInstanceServiceTest {
  private val appointmentInstanceRepository: AppointmentInstanceRepository = mock()

  private val appointmentInstanceService = AppointmentInstanceService(appointmentInstanceRepository)

  @Nested
  @DisplayName("getAppointmentInstanceById")
  inner class GetAppointmentInstanceById {
    @Test
    fun `returns an appointment instance for known appointment instance id`() {
      val entity = appointmentInstanceEntity()
      whenever(appointmentInstanceRepository.findById(1)).thenReturn(Optional.of(entity))
      assertThat(appointmentInstanceService.getAppointmentInstanceById(1)).isEqualTo(entity.toModel())
    }

    @Test
    fun `throws entity not found exception for unknown appointment instance id`() {
      assertThatThrownBy { appointmentInstanceService.getAppointmentInstanceById(0) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Appointment Instance 0 not found")
    }
  }
}
