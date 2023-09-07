package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CancelAppointmentOccurrencesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UpdateAppointmentOccurrencesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.time.LocalDate
import java.util.Optional

class AppointmentOccurrenceServiceUpdateTest {
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val appointmentOccurrenceUpdateDomainService: AppointmentOccurrenceUpdateDomainService = mock()
  private val appointmentOccurrenceCancelDomainService: AppointmentOccurrenceCancelDomainService = mock()
  private val updateAppointmentOccurrencesJob: UpdateAppointmentOccurrencesJob = mock()
  private val cancelAppointmentOccurrencesJob: CancelAppointmentOccurrencesJob = mock()

  private val service = AppointmentOccurrenceService(
    appointmentOccurrenceRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    appointmentOccurrenceUpdateDomainService,
    appointmentOccurrenceCancelDomainService,
    updateAppointmentOccurrencesJob,
    cancelAppointmentOccurrencesJob,
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
  fun `updateAppointmentOccurrence throws entity not found exception for unknown appointment occurrence id`() {
    assertThatThrownBy { service.updateAppointmentOccurrence(-1, AppointmentOccurrenceUpdateRequest(), mock()) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment Occurrence -1 not found")
  }

  @Test
  fun `update appointment throws caseload access exception if caseload id header does not match`() {
    val appointmentOccurrence = expectIndividualAppointment()
    addCaseloadIdToRequestHeader("WRONG")
    val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456, applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)
    assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal) }
      .isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `update category code throws illegal argument exception when requested category code is not found`() {
    val appointmentOccurrence = expectGroupAppointment()
    val request = AppointmentOccurrenceUpdateRequest(categoryCode = "NOT_FOUND")

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)).thenReturn(emptyMap())

    assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment Category with code ${request.categoryCode} not found or is not active")
  }

  @Test
  fun `update internal location throws illegal argument exception when inCell = false and requested internal location id is not found`() {
    val appointmentOccurrence = expectGroupAppointment()
    val appointment = appointmentOccurrence.appointmentSeries
    val request = AppointmentOccurrenceUpdateRequest(internalLocationId = -1)

    whenever(locationService.getLocationsForAppointmentsMap(appointment.prisonCode)).thenReturn(emptyMap())

    assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment location with id ${request.internalLocationId} not found in prison '${appointment.prisonCode}'")
  }

  @Test
  fun `update prisoner list throws illegal argument exception when prisoner is not found`() {
    val appointmentOccurrence = expectGroupAppointment()
    val request = AppointmentOccurrenceUpdateRequest(addPrisonerNumbers = listOf("NOT_FOUND"))

    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!)).thenReturn(Mono.just(emptyList()))

    assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.addPrisonerNumbers!!.first()}' not found, were inactive or are residents of a different prison.")
  }

  @Test
  fun `update prisoner list throws illegal argument exception when prisoner is not a resident of requested prison code`() {
    val appointmentOccurrence = expectGroupAppointment()
    val request = AppointmentOccurrenceUpdateRequest(addPrisonerNumbers = listOf("DIFFERENT_PRISON"))

    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!))
      .thenReturn(Mono.just(listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.addPrisonerNumbers!!.first(), prisonId = "DIFFERENT"))))

    assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.addPrisonerNumbers!!.first()}' not found, were inactive or are residents of a different prison.")
  }

  @Test
  fun `update prisoner list throws illegal argument exception when adding prisoner to individual appointment`() {
    val appointmentOccurrence = expectIndividualAppointment()
    val request = AppointmentOccurrenceUpdateRequest(addPrisonerNumbers = listOf("A1234BC", "BC2345D"))

    assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add prisoners to an individual appointment occurrence")
  }

  private fun expectGroupAppointment(): Appointment {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now().plusDays(1), updatedBy = null, appointmentType = AppointmentType.GROUP)
    val appointmentOccurrence = appointmentSeries.appointments().first()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )
    return appointmentOccurrence
  }

  private fun expectIndividualAppointment(): Appointment {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now().plusDays(1), updatedBy = null, appointmentType = AppointmentType.INDIVIDUAL)
    val appointmentOccurrence = appointmentSeries.appointments().first()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )
    return appointmentOccurrence
  }
}
