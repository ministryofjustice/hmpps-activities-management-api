package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceAllocationRepository

class AppointmentOccurrenceAllocationServiceTest {

  private val prisonApiClient = mock<PrisonApiClient>()
  private val appointmentInstanceRepository = mock<AppointmentInstanceRepository>()
  private val appointmentOccurrenceAllocationRepository = mock<AppointmentOccurrenceAllocationRepository>()
  private val appointmentOccurrenceAllocationService = AppointmentOccurrenceAllocationService(
    prisonApiClient,
    appointmentInstanceRepository,
    appointmentOccurrenceAllocationRepository,
  )

  @Test
  fun `cancels all future appointments`() {
    val appointmentInstanceId = 42L
    val prisonCode = "PVI"
    val prisonerNumber = "ABC123"
    val inmateDetail = mock<InmateDetail>()
    val appointmentInstance = mock<AppointmentInstance>()

    whenever(
      prisonApiClient.getPrisonerDetails(
        prisonerNumber = prisonerNumber,
        fullInfo = true,
        extraInfo = true,
      ),
    ).thenReturn(Mono.just(inmateDetail))

    whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromToday(prisonCode, prisonerNumber))
      .thenReturn(listOf(appointmentInstance))

    whenever(appointmentInstance.appointmentOccurrenceAllocationId).thenReturn(appointmentInstanceId)

    appointmentOccurrenceAllocationService.cancelFutureOffenderAppointments(prisonCode, prisonerNumber)

    verify(appointmentOccurrenceAllocationRepository).deleteById(appointmentInstanceId)
  }

  @Test
  fun `does not cancel anything if there are no future appointments`() {
    val prisonCode = "PVI"
    val prisonerNumber = "ABC123"
    val inmateDetail = mock<InmateDetail>()

    whenever(
      prisonApiClient.getPrisonerDetails(
        prisonerNumber = prisonerNumber,
        fullInfo = true,
        extraInfo = true,
      ),
    ).thenReturn(Mono.just(inmateDetail))

    whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromToday(prisonCode, prisonerNumber))
      .thenReturn(listOf())

    appointmentOccurrenceAllocationService.cancelFutureOffenderAppointments(prisonCode, prisonerNumber)

    verifyNoInteractions(appointmentOccurrenceAllocationRepository)
  }
}
