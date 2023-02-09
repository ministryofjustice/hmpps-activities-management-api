package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceAllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import java.util.Optional

class AppointmentServiceTest {
  private val appointmentCategoryRepository: AppointmentCategoryRepository = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository = mock()
  private val appointmentOccurrenceAllocationRepository: AppointmentOccurrenceAllocationRepository = mock()
  private val appointmentInstanceRepository: AppointmentInstanceRepository = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()

  private val service = AppointmentService(
    appointmentCategoryRepository,
    appointmentRepository,
    appointmentOccurrenceRepository,
    appointmentOccurrenceAllocationRepository,
    appointmentInstanceRepository,
    prisonerSearchApiClient
  )

  @Test
  fun `getAppointmentById returns an appointment for known appointment id`() {
    val entity = appointmentEntity()
    whenever(appointmentRepository.findById(1)).thenReturn(Optional.of(entity))
    assertThat(service.getAppointmentById(1)).isEqualTo(entity.toModel())
  }

  @Test
  fun `getAppointmentById throws entity not found exception for unknown appointment id`() {
    assertThatThrownBy { service.getAppointmentById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment -1 not found")
  }
}
