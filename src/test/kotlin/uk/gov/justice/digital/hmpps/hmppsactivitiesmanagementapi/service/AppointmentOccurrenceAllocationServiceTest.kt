package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceAllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import java.util.Optional

class AppointmentOccurrenceAllocationServiceTest {

  private val prisonApiClient = mock<PrisonApiApplicationClient>()
  private val appointmentInstanceRepository = mock<AppointmentInstanceRepository>()
  private val appointmentOccurrenceAllocationRepository = mock<AppointmentOccurrenceAllocationRepository>()
  private val appointmentOccurrenceRepository = mock<AppointmentOccurrenceRepository>()
  private val appointmentOccurrenceAllocationService = AppointmentOccurrenceAllocationService(
    prisonApiClient,
    appointmentInstanceRepository,
    appointmentOccurrenceRepository,
    appointmentOccurrenceAllocationRepository,
  )

  @Test
  fun `cancels all future appointments`() {
    val appointmentInstanceId = 42L
    val prisonCode = "PVI"
    val prisonerNumber = "ABC123"
    val inmateDetail = mock<InmateDetail>()
    val appointmentInstance = mock<AppointmentInstance>()

    whenever(appointmentInstance.appointmentOccurrenceAllocationId).thenReturn(appointmentInstanceId)

    whenever(
      prisonApiClient.getPrisonerDetails(
        prisonerNumber = prisonerNumber,
        fullInfo = true,
        extraInfo = true,
      ),
    ).thenReturn(Mono.just(inmateDetail))

    whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromToday(prisonCode, prisonerNumber))
      .thenReturn(listOf(appointmentInstance))

    whenever(appointmentOccurrenceAllocationRepository.findById(appointmentInstanceId)).thenReturn(Optional.empty())

    appointmentOccurrenceAllocationService.cancelFutureOffenderAppointments(prisonCode, prisonerNumber)

    verify(appointmentOccurrenceAllocationRepository).deleteById(appointmentInstanceId)
  }

  @Test
  fun `cancels all future appointments and deletes orphaned ocurrences`() {
    val appointmentOccurrenceAllocationId = 42L
    val prisonCode = "PVI"
    val prisonerNumber = "ABC123"
    val inmateDetail = mock<InmateDetail>()
    val appointmentInstance = mock<AppointmentInstance>()
    val parentAllocation = mock<AppointmentOccurrenceAllocation>()
    val parentOccurrence = mock<AppointmentOccurrence>()

    whenever(appointmentInstance.appointmentOccurrenceAllocationId).thenReturn(appointmentOccurrenceAllocationId)

    whenever(
      prisonApiClient.getPrisonerDetails(
        prisonerNumber = prisonerNumber,
        fullInfo = true,
        extraInfo = true,
      ),
    ).thenReturn(Mono.just(inmateDetail))

    whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromToday(prisonCode, prisonerNumber))
      .thenReturn(listOf(appointmentInstance))

    whenever(appointmentOccurrenceAllocationRepository.findById(appointmentOccurrenceAllocationId)).thenReturn(Optional.of(parentAllocation))
    whenever(parentAllocation.appointmentOccurrence).thenReturn(parentOccurrence)
    whenever(parentAllocation.appointmentOccurrenceAllocationId).thenReturn(appointmentOccurrenceAllocationId)
    whenever(parentOccurrence.allocations()).thenReturn(listOf(parentAllocation))

    appointmentOccurrenceAllocationService.cancelFutureOffenderAppointments(prisonCode, prisonerNumber)

    verify(appointmentOccurrenceAllocationRepository).deleteById(appointmentOccurrenceAllocationId)
    verify(appointmentOccurrenceRepository).delete(parentOccurrence)
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
