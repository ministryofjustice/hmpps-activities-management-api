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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import java.util.Optional

@ExtendWith(FakeSecurityContext::class)
class AppointmentAttendeeServiceTest {

  private val prisonApiClient = mock<PrisonApiApplicationClient>()
  private val appointmentInstanceRepository = mock<AppointmentInstanceRepository>()
  private val appointmentAttendeeRepository = mock<AppointmentAttendeeRepository>()
  private val auditService = mock<AuditService>()

  private val appointmentAttendeeService = AppointmentAttendeeService(
    prisonApiClient,
    appointmentInstanceRepository,
    appointmentAttendeeRepository,
    auditService,
  )

  @Test
  fun `cancels all future appointments`() {
    val appointmentAttendeeId = 42L
    val prisonCode = "PVI"
    val prisonerNumber = "ABC123"
    val inmateDetail = mock<InmateDetail>()
    val appointmentInstance = mock<AppointmentInstance>()
    val parentAppointmentSeries = mock<AppointmentSeries>()
    val parentAppointmentAttendee = mock<AppointmentAttendee>()
    val parentAppointment = mock<Appointment>()

    whenever(appointmentInstance.appointmentAttendeeId).thenReturn(appointmentAttendeeId)
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

    whenever(appointmentAttendeeRepository.findById(appointmentAttendeeId)).thenReturn(Optional.of(parentAppointmentAttendee))
    whenever(parentAppointmentAttendee.appointment).thenReturn(parentAppointment)
    whenever(parentAppointmentAttendee.appointmentAttendeeId).thenReturn(appointmentAttendeeId)
    whenever(parentAppointment.appointmentSeries).thenReturn(parentAppointmentSeries)
    whenever(parentAppointmentAttendee.isIndividualAppointment()).thenReturn(false)

    appointmentAttendeeService.cancelFutureOffenderAppointments(prisonCode, prisonerNumber)

    verify(parentAppointmentAttendee).removeFromAppointment()
  }

  @Test
  fun `cancels all future appointments and deletes orphaned individual appointment `() {
    val appointmentAttendeeId = 42L
    val prisonCode = "PVI"
    val prisonerNumber = "ABC123"
    val inmateDetail = mock<InmateDetail>()
    val appointmentInstance = mock<AppointmentInstance>()
    val parentAppointmentSeries = mock<AppointmentSeries>()
    val parentAppointmentAttendee = mock<AppointmentAttendee>()
    val parentAppointment = mock<Appointment>()

    whenever(appointmentInstance.appointmentAttendeeId).thenReturn(appointmentAttendeeId)
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

    whenever(appointmentAttendeeRepository.findById(appointmentAttendeeId)).thenReturn(Optional.of(parentAppointmentAttendee))
    whenever(parentAppointmentAttendee.appointment).thenReturn(parentAppointment)
    whenever(parentAppointmentAttendee.appointmentAttendeeId).thenReturn(appointmentAttendeeId)
    whenever(parentAppointment.appointmentSeries).thenReturn(parentAppointmentSeries)
    whenever(parentAppointmentAttendee.isIndividualAppointment()).thenReturn(true)

    appointmentAttendeeService.cancelFutureOffenderAppointments(prisonCode, prisonerNumber)

    verify(parentAppointmentAttendee).removeAppointment(parentAppointment)
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

    appointmentAttendeeService.cancelFutureOffenderAppointments(prisonCode, prisonerNumber)

    verifyNoInteractions(appointmentAttendeeRepository)
  }
}
