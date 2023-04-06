package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.*
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.lang.IllegalArgumentException
import java.security.Principal
import java.util.*

class AppointmentOccurrenceServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val outboundEventsService: OutboundEventsService = mock()

  private val service = AppointmentOccurrenceService(
    appointmentRepository,
    appointmentOccurrenceRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    outboundEventsService,
  )

  @Test
  fun `updateAppointmentOccurrence throws entity not found exception for unknown appointment occurrence id`() {
    assertThatThrownBy { service.updateAppointmentOccurrence(-1, AppointmentOccurrenceUpdateRequest(), mock()) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment Occurrence -1 not found")
  }

  @Test
  fun `update category code throws illegal argument exception when requested category code is not found`() {
    val request = AppointmentOccurrenceUpdateRequest(categoryCode = "NOT_FOUND")
    val principal: Principal = mock()

    val appointment = appointmentEntity(updatedBy = null)
    val appointmentOccurrence = appointment.occurrences().first()

    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)).thenReturn(emptyMap())

    assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment Category with code ${request.categoryCode} not found or is not active")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `update internal location throws illegal argument exception when inCell = false and requested internal location id is not found`() {
    val request = AppointmentOccurrenceUpdateRequest(internalLocationId = -1)
    val principal: Principal = mock()

    val appointment = appointmentEntity(updatedBy = null)
    val appointmentOccurrence = appointment.occurrences().first()

    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence))
    whenever(locationService.getLocationsForAppointmentsMap(appointment.prisonCode)).thenReturn(emptyMap())

    assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment location with id ${request.internalLocationId} not found in prison '${appointment.prisonCode}'")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createAppointment throws illegal argument exception when prisoner is not found`() {
    val request = AppointmentOccurrenceUpdateRequest(prisonerNumbers = listOf("NOT_FOUND"))
    val principal: Principal = mock()

    val appointment = appointmentEntity(updatedBy = null)
    val appointmentOccurrence = appointment.occurrences().first()

    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers!!)).thenReturn(Mono.just(emptyList()))

    assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.prisonerNumbers!!.first()}' not found, were inactive or are residents of a different prison.")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createAppointment throws illegal argument exception when prisoner is not a resident of requested prison code`() {
    val request = AppointmentOccurrenceUpdateRequest(prisonerNumbers = listOf("DIFFERENT_PRISON"))
    val principal: Principal = mock()

    val appointment = appointmentEntity(updatedBy = null)
    val appointmentOccurrence = appointment.occurrences().first()

    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers!!))
      .thenReturn(Mono.just(listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers!!.first(), prisonId = "DIFFERENT"))))

    assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.prisonerNumbers!!.first()}' not found, were inactive or are residents of a different prison.")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Nested
  @DisplayName("update individual appointment")
  inner class UpdateIndividualAppointment {
    val appointment = appointmentEntity(updatedBy = null)
    val appointmentOccurrence = appointment.occurrences().first()

    @BeforeEach
    fun setUp() {
      whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
        Optional.of(appointmentOccurrence))
    }

    @Test
    fun `update category code throws illegal argument exception when requested category code is not found`() {
      val request = AppointmentOccurrenceUpdateRequest(categoryCode = "NOT_FOUND")
      val principal: Principal = mock()

      whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)).thenReturn(emptyMap())

      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Appointment Category with code ${request.categoryCode} not found or is not active")

      verify(appointmentRepository, never()).saveAndFlush(any())
    }
  }
}