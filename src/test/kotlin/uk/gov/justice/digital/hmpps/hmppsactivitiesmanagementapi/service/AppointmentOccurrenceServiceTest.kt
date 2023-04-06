package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.AdditionalAnswers.returnsFirstArg
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Optional

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

  @Nested
  @DisplayName("update appointment occurrence validation")
  inner class UpdateAppointmentOccurrenceValidation {
    private val principal: Principal = mock()
    private val appointment = appointmentEntity(updatedBy = null)
    private val appointmentOccurrence = appointment.occurrences().first()

    @BeforeEach
    fun setUp() {
      whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
        Optional.of(appointmentOccurrence),
      )
    }

    @Test
    fun `updateAppointmentOccurrence throws entity not found exception for unknown appointment occurrence id`() {
      assertThatThrownBy { service.updateAppointmentOccurrence(-1, AppointmentOccurrenceUpdateRequest(), mock()) }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Appointment Occurrence -1 not found")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update category code throws illegal argument exception when requested category code is not found`() {
      val request = AppointmentOccurrenceUpdateRequest(categoryCode = "NOT_FOUND")

      whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)).thenReturn(emptyMap())

      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Appointment Category with code ${request.categoryCode} not found or is not active")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update internal location throws illegal argument exception when inCell = false and requested internal location id is not found`() {
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = -1)

      whenever(locationService.getLocationsForAppointmentsMap(appointment.prisonCode)).thenReturn(emptyMap())

      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Appointment location with id ${request.internalLocationId} not found in prison '${appointment.prisonCode}'")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update prisoner list throws illegal argument exception when prisoner is not found`() {
      val request = AppointmentOccurrenceUpdateRequest(prisonerNumbers = listOf("NOT_FOUND"))

      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers!!)).thenReturn(Mono.just(emptyList()))

      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Prisoner(s) with prisoner number(s) '${request.prisonerNumbers!!.first()}' not found, were inactive or are residents of a different prison.")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update prisoner list throws illegal argument exception when prisoner is not a resident of requested prison code`() {
      val request = AppointmentOccurrenceUpdateRequest(prisonerNumbers = listOf("DIFFERENT_PRISON"))
      val principal: Principal = mock()

      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers!!))
        .thenReturn(Mono.just(listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers!!.first(), prisonId = "DIFFERENT"))))

      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Prisoner(s) with prisoner number(s) '${request.prisonerNumbers!!.first()}' not found, were inactive or are residents of a different prison.")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update prisoner list throws illegal argument exception when adding prisoner to individual appointment`() {
      val request = AppointmentOccurrenceUpdateRequest(prisonerNumbers = listOf("A1234BC", "BC2345D"))

      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot allocate more than one prisoner to an individual appointment occurrence")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }
  }

  @Nested
  @DisplayName("update individual appointment")
  inner class UpdateIndividualAppointment {
    private val principal: Principal = mock()
    private val appointment = appointmentEntity(updatedBy = null)
    private val appointmentOccurrence = appointment.occurrences().first()
    private val appointmentOccurrenceAllocation = appointmentOccurrence.allocations().first()

    @BeforeEach
    fun setUp() {
      whenever(principal.name).thenReturn("TEST.USER")
      whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
        Optional.of(appointmentOccurrence),
      )
      whenever(appointmentRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg<Appointment>())
    }

    @Test
    fun `update appointment category code success`() {
      val request = AppointmentOccurrenceUpdateRequest(categoryCode = "NEW")

      whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
        .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!, "New Category")))

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(categoryCode).isEqualTo(request.categoryCode)
        assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
        assertThat(updatedBy).isEqualTo("TEST.USER")
        with(response.occurrences.single()) {
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `update internal location id success`() {
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)

      whenever(locationService.getLocationsForAppointmentsMap(appointment.prisonCode))
        .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, appointment.prisonCode)))

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(internalLocationId).isEqualTo(123)
        assertThat(inCell).isFalse
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(response.occurrences.single()) {
          assertThat(internalLocationId).isEqualTo(request.internalLocationId)
          assertThat(inCell).isFalse
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `update in cell = true success`() {
      val request = AppointmentOccurrenceUpdateRequest(inCell = true)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(internalLocationId).isEqualTo(123)
        assertThat(inCell).isFalse
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(response.occurrences.single()) {
          assertThat(internalLocationId).isNull()
          assertThat(inCell).isTrue
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `update start date success`() {
      val request = AppointmentOccurrenceUpdateRequest(startDate = LocalDate.now().plusWeeks(1))

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(startDate).isEqualTo(LocalDate.now())
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(response.occurrences.single()) {
          assertThat(startDate).isEqualTo(request.startDate)
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `update start time success`() {
      val request = AppointmentOccurrenceUpdateRequest(startTime = LocalTime.of(13, 30))

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(response.occurrences.single()) {
          assertThat(startTime).isEqualTo(request.startTime)
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `update end time success`() {
      val request = AppointmentOccurrenceUpdateRequest(endTime = LocalTime.of(15, 0))

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(response.occurrences.single()) {
          assertThat(endTime).isEqualTo(request.endTime)
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `update comment success`() {
      val request = AppointmentOccurrenceUpdateRequest(comment = "Updated appointment occurrence level comment")

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(comment).isEqualTo("Appointment level comment")
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(response.occurrences.single()) {
          assertThat(comment).isEqualTo(request.comment)
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `update prisoner with no changes success`() {
      val request = AppointmentOccurrenceUpdateRequest(prisonerNumbers = appointmentOccurrence.prisonerNumbers())

      var index = 0
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers!!))
        .thenReturn(
          Mono.just(
            request.prisonerNumbers!!.map {
              PrisonerSearchPrisonerFixture.instance(prisonerNumber = it, bookingId = 456L + index++, prisonId = appointment.prisonCode)
            },
          ),
        )

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.single()) {
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
          with(allocations.single()) {
            assertThat(prisonerNumber).isEqualTo("A1234BC")
            assertThat(bookingId).isEqualTo(456)
          }
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `update prisoner success`() {
      val request = AppointmentOccurrenceUpdateRequest(prisonerNumbers = listOf("BC2345D"))

      var index = 1L
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers!!))
        .thenReturn(
          Mono.just(
            request.prisonerNumbers!!.map {
              PrisonerSearchPrisonerFixture.instance(prisonerNumber = it, bookingId = index++, prisonId = appointment.prisonCode)
            },
          ),
        )

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.single()) {
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
          with(allocations.single()) {
            assertThat(prisonerNumber).isEqualTo("BC2345D")
            assertThat(bookingId).isEqualTo(1)
          }
        }
      }

      verifyNoInteractions(outboundEventsService)
    }
  }
}
