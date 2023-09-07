package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceAllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import java.util.Optional

@ExtendWith(FakeSecurityContext::class)
class AppointmentOccurrenceAllocationServiceTest {

  private val prisonApiClient = mock<PrisonApiApplicationClient>()
  private val appointmentInstanceRepository = mock<AppointmentInstanceRepository>()
  private val appointmentOccurrenceAllocationRepository = mock<AppointmentOccurrenceAllocationRepository>()
  private val auditService = mock<AuditService>()

  private val appointmentOccurrenceAllocationService = AppointmentOccurrenceAllocationService(
    prisonApiClient,
    appointmentInstanceRepository,
    appointmentOccurrenceAllocationRepository,
    auditService,
  )

  @Test
  fun `cancels all future appointments`() {
    val appointmentOccurrenceAllocationId = 42L
    val prisonCode = "PVI"
    val prisonerNumber = "ABC123"
    val inmateDetail = mock<InmateDetail>()
    val appointmentInstance = mock<AppointmentInstance>()
    val parentAppointmentSeries = mock<AppointmentSeries>()
    val parentAllocation = mock<AppointmentOccurrenceAllocation>()
    val parentOccurrence = mock<AppointmentOccurrence>()

    whenever(appointmentInstance.appointmentOccurrenceAllocationId).thenReturn(appointmentOccurrenceAllocationId)
    whenever(appointmentInstance.prisonCode).thenReturn(prisonCode)
    whenever(appointmentInstance.prisonerNumber).thenReturn(prisonerNumber)

    whenever(
      prisonApiClient.getPrisonerDetails(
        prisonerNumber = prisonerNumber,
        fullInfo = true,
        extraInfo = true,
      ),
    ).thenReturn(Mono.just(inmateDetail))

    whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber))
      .thenReturn(listOf(appointmentInstance))

    whenever(appointmentOccurrenceAllocationRepository.findById(appointmentOccurrenceAllocationId)).thenReturn(Optional.of(parentAllocation))
    whenever(parentAllocation.appointmentOccurrence).thenReturn(parentOccurrence)
    whenever(parentAllocation.appointmentOccurrenceAllocationId).thenReturn(appointmentOccurrenceAllocationId)
    whenever(parentOccurrence.appointmentSeries).thenReturn(parentAppointmentSeries)
    whenever(parentAllocation.isIndividualAppointment()).thenReturn(false)

    appointmentOccurrenceAllocationService.cancelFutureOffenderAppointments(prisonCode, prisonerNumber)

    verify(parentAllocation).removeFromAppointmentOccurrence()
  }

  @Test
  fun `cancels all future appointments and deletes orphaned individual appointment `() {
    val appointmentOccurrenceAllocationId = 42L
    val prisonCode = "PVI"
    val prisonerNumber = "ABC123"
    val inmateDetail = mock<InmateDetail>()
    val appointmentInstance = mock<AppointmentInstance>()
    val parentAppointmentSeries = mock<AppointmentSeries>()
    val parentAllocation = mock<AppointmentOccurrenceAllocation>()
    val parentOccurrence = mock<AppointmentOccurrence>()

    whenever(appointmentInstance.appointmentOccurrenceAllocationId).thenReturn(appointmentOccurrenceAllocationId)
    whenever(appointmentInstance.prisonCode).thenReturn(prisonCode)
    whenever(appointmentInstance.prisonerNumber).thenReturn(prisonerNumber)

    whenever(
      prisonApiClient.getPrisonerDetails(
        prisonerNumber = prisonerNumber,
        fullInfo = true,
        extraInfo = true,
      ),
    ).thenReturn(Mono.just(inmateDetail))

    whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber))
      .thenReturn(listOf(appointmentInstance))

    whenever(appointmentOccurrenceAllocationRepository.findById(appointmentOccurrenceAllocationId)).thenReturn(Optional.of(parentAllocation))
    whenever(parentAllocation.appointmentOccurrence).thenReturn(parentOccurrence)
    whenever(parentAllocation.appointmentOccurrenceAllocationId).thenReturn(appointmentOccurrenceAllocationId)
    whenever(parentOccurrence.appointmentSeries).thenReturn(parentAppointmentSeries)
    whenever(parentAllocation.isIndividualAppointment()).thenReturn(true)

    appointmentOccurrenceAllocationService.cancelFutureOffenderAppointments(prisonCode, prisonerNumber)

    verify(parentAllocation).removeOccurrence(parentOccurrence)
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

    whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber))
      .thenReturn(listOf())

    appointmentOccurrenceAllocationService.cancelFutureOffenderAppointments(prisonCode, prisonerNumber)

    verifyNoInteractions(appointmentOccurrenceAllocationRepository)
  }
}
