package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CancelAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UpdateAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.time.LocalDate
import java.util.Optional

class AppointmentServiceUpdateTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val appointmentUpdateDomainService: AppointmentUpdateDomainService = mock()
  private val appointmentCancelDomainService: AppointmentCancelDomainService = mock()
  private val updateAppointmentsJob: UpdateAppointmentsJob = mock()
  private val cancelAppointmentsJob: CancelAppointmentsJob = mock()

  private val service = AppointmentService(
    appointmentRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    prisonApiClient,
    appointmentUpdateDomainService,
    appointmentCancelDomainService,
    updateAppointmentsJob,
    cancelAppointmentsJob,
    maxSyncAppointmentInstanceActions = 14,
  )

  private val principal: Principal = mock()

  @BeforeEach
  fun setup() {
    addCaseloadIdToRequestHeader("TPR")
    whenever(principal.name).thenReturn("TEST.USER")
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Test
  fun `updateAppointment throws entity not found exception for unknown appointment id`() {
    assertThatThrownBy { service.updateAppointment(-1, AppointmentUpdateRequest(), mock()) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment -1 not found")
  }

  @Test
  fun `update appointment throws caseload access exception if caseload id header does not match`() {
    val appointment = expectIndividualAppointment()
    addCaseloadIdToRequestHeader("WRONG")
    val request = AppointmentUpdateRequest(internalLocationId = 456, applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS)
    assertThatThrownBy { service.updateAppointment(appointment.appointmentId, request, principal) }
      .isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `update category code throws illegal argument exception when requested category code is not found`() {
    val appointment = expectGroupAppointment()
    val request = AppointmentUpdateRequest(categoryCode = "NOT_FOUND")

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)).thenReturn(emptyMap())

    assertThatThrownBy { service.updateAppointment(appointment.appointmentId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment Category with code ${request.categoryCode} not found or is not active")
  }

  @Test
  fun `update internal location throws illegal argument exception when inCell = false and requested internal location id is not found`() {
    val appointment = expectGroupAppointment()
    val appointmentSeries = appointment.appointmentSeries
    val request = AppointmentUpdateRequest(internalLocationId = -1)

    whenever(locationService.getLocationsForAppointmentsMap(appointmentSeries.prisonCode)).thenReturn(emptyMap())

    assertThatThrownBy { service.updateAppointment(appointment.appointmentId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment location with id ${request.internalLocationId} not found in prison '${appointmentSeries.prisonCode}'")
  }

  @Test
  fun `update prisoner list throws illegal argument exception when prisoner is not found`() {
    val appointment = expectGroupAppointment()
    val request = AppointmentUpdateRequest(addPrisonerNumbers = listOf("NOT_FOUND"))

    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!)).thenReturn(emptyList())

    assertThatThrownBy { service.updateAppointment(appointment.appointmentId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.addPrisonerNumbers!!.first()}' not found, were inactive or are residents of a different prison.")
  }

  @Test
  fun `update prisoner list throws illegal argument exception when prisoner is not a resident of requested prison code`() {
    val appointment = expectGroupAppointment()
    val request = AppointmentUpdateRequest(addPrisonerNumbers = listOf("DIFFERENT_PRISON"))

    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!))
      .thenReturn(listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.addPrisonerNumbers!!.first(), prisonId = "DIFFERENT")))

    assertThatThrownBy { service.updateAppointment(appointment.appointmentId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.addPrisonerNumbers!!.first()}' not found, were inactive or are residents of a different prison.")
  }

  @Test
  fun `update prisoner list throws illegal argument exception when adding prisoner to individual appointment`() {
    val appointment = expectIndividualAppointment()
    val request = AppointmentUpdateRequest(addPrisonerNumbers = listOf("A1234BC", "BC2345D"))

    assertThatThrownBy { service.updateAppointment(appointment.appointmentId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add prisoners to an individual appointment")
  }

  @Test
  fun `updating prisoner list throws exception if adding more than 20,000 new appointment instances`() {
    val appointment = expectRepeatAppointment()
    val appointmentSeries = appointment.appointmentSeries
    val prisonerList = MutableList(250) { prisoner -> "A11${prisoner}BC" }
    val request = AppointmentUpdateRequest(addPrisonerNumbers = prisonerList, applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS)

    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!))
      .thenReturn(
        request.addPrisonerNumbers!!.map {
          PrisonerSearchPrisonerFixture.instance(
            prisonerNumber = it,
            bookingId = 1,
            prisonId = appointment.prisonCode,
          )
        },
      )

    whenever(appointmentUpdateDomainService.getUpdateInstancesCount(request, appointmentSeries, appointmentSeries.appointments()))
      .thenReturn(prisonerList.size * appointmentSeries.schedule!!.numberOfAppointments)

    assertThatThrownBy { service.updateAppointment(appointment.appointmentId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("You cannot modify more than 200 appointment instances for this number of attendees")
  }

  private fun expectRepeatAppointment(): Appointment {
    val appointmentSeries = appointmentSeriesEntity(
      appointmentType = AppointmentType.GROUP,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 100,
    )
    val appointment = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )
    return appointment
  }

  private fun expectGroupAppointment(): Appointment {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now().plusDays(1), updatedBy = null, appointmentType = AppointmentType.GROUP)
    val appointment = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )
    return appointment
  }

  private fun expectIndividualAppointment(): Appointment {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now().plusDays(1), updatedBy = null, appointmentType = AppointmentType.INDIVIDUAL)
    val appointment = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )
    return appointment
  }
}
