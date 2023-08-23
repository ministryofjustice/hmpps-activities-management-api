package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.AdditionalAnswers.returnsFirstArg
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancellationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLY_TO_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.END_TIME_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EXTRA_INFORMATION_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_ADDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_REMOVED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_NAME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_DATE_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_TIME_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Optional

class AppointmentOccurrenceServiceUpdateTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository = mock()
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val telemetryClient: TelemetryClient = mock()

  @Captor
  private lateinit var telemetryPropertyMap: ArgumentCaptor<Map<String, String>>

  @Captor
  private lateinit var telemetryMetricsMap: ArgumentCaptor<Map<String, Double>>

  private val service = AppointmentOccurrenceService(
    appointmentRepository,
    appointmentOccurrenceRepository,
    appointmentCancellationReasonRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    outboundEventsService,
    telemetryClient,
  )

  @BeforeEach
  fun setup() {
    MockitoAnnotations.openMocks(this)
    addCaseloadIdToRequestHeader("TPR")
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Nested
  @DisplayName("update appointment occurrence validation")
  inner class UpdateAppointmentOccurrenceValidation {
    private val principal: Principal = mock()

    @BeforeEach
    fun setUp() {
      whenever(principal.name).thenReturn("TEST.USER")
    }

    fun expectGroupAppointment(): AppointmentOccurrence {
      val appointment = appointmentEntity(startDate = LocalDate.now().plusDays(1), updatedBy = null, appointmentType = AppointmentType.GROUP)
      val appointmentOccurrence = appointment.occurrences().first()
      whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
        Optional.of(appointmentOccurrence),
      )
      return appointmentOccurrence
    }

    fun expectIndividualAppointment(): AppointmentOccurrence {
      val appointment = appointmentEntity(startDate = LocalDate.now().plusDays(1), updatedBy = null, appointmentType = AppointmentType.INDIVIDUAL)
      val appointmentOccurrence = appointment.occurrences().first()
      whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
        Optional.of(appointmentOccurrence),
      )
      return appointmentOccurrence
    }

    @Test
    fun `updateAppointmentOccurrence throws entity not found exception for unknown appointment occurrence id`() {
      assertThatThrownBy { service.updateAppointmentOccurrence(-1, AppointmentOccurrenceUpdateRequest(), mock()) }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Appointment Occurrence -1 not found")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update category code throws illegal argument exception when appointment occurrence is in the past`() {
      val request = AppointmentOccurrenceUpdateRequest()

      val appointment = appointmentEntity(appointmentId = 2, startDate = LocalDate.now(), startTime = LocalTime.now().minusMinutes(1), endTime = LocalTime.now().plusHours(1))
      val appointmentOccurrence = appointment.occurrences().first()

      whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
        Optional.of(appointmentOccurrence),
      )

      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot update a past appointment occurrence")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update category code throws illegal argument exception when appointment occurrence is cancelled`() {
      val request = AppointmentOccurrenceUpdateRequest()

      val appointment = appointmentEntity()
      val appointmentOccurrence = appointment.occurrences().first()
      appointmentOccurrence.cancellationReason = AppointmentCancellationReason(2L, "Cancelled", false)

      whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
        Optional.of(appointmentOccurrence),
      )

      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot update a cancelled appointment occurrence")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update category code throws illegal argument exception when requested category code is not found`() {
      val appointmentOccurrence = expectGroupAppointment()
      val request = AppointmentOccurrenceUpdateRequest(categoryCode = "NOT_FOUND")

      whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)).thenReturn(emptyMap())

      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Appointment Category with code ${request.categoryCode} not found or is not active")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update internal location throws illegal argument exception when inCell = false and requested internal location id is not found`() {
      val appointmentOccurrence = expectGroupAppointment()
      val appointment = appointmentOccurrence.appointment
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = -1)

      whenever(locationService.getLocationsForAppointmentsMap(appointment.prisonCode)).thenReturn(emptyMap())

      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Appointment location with id ${request.internalLocationId} not found in prison '${appointment.prisonCode}'")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update prisoner list throws illegal argument exception when prisoner is not found`() {
      val appointmentOccurrence = expectGroupAppointment()
      val request = AppointmentOccurrenceUpdateRequest(addPrisonerNumbers = listOf("NOT_FOUND"))

      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!)).thenReturn(Mono.just(emptyList()))

      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Prisoner(s) with prisoner number(s) '${request.addPrisonerNumbers!!.first()}' not found, were inactive or are residents of a different prison.")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update prisoner list throws illegal argument exception when prisoner is not a resident of requested prison code`() {
      val appointmentOccurrence = expectGroupAppointment()
      val request = AppointmentOccurrenceUpdateRequest(addPrisonerNumbers = listOf("DIFFERENT_PRISON"))

      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!))
        .thenReturn(Mono.just(listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.addPrisonerNumbers!!.first(), prisonId = "DIFFERENT"))))

      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Prisoner(s) with prisoner number(s) '${request.addPrisonerNumbers!!.first()}' not found, were inactive or are residents of a different prison.")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update prisoner list throws illegal argument exception when adding prisoner to individual appointment`() {
      val appointmentOccurrence = expectIndividualAppointment()
      val request = AppointmentOccurrenceUpdateRequest(addPrisonerNumbers = listOf("A1234BC", "BC2345D"))

      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot add prisoners to an individual appointment occurrence")

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(outboundEventsService)
    }
  }

  @Nested
  @DisplayName("update individual appointment")
  inner class UpdateIndividualAppointment {
    private val principal: Principal = mock()
    private val appointment = appointmentEntity(startDate = LocalDate.now().plusDays(1), updatedBy = null, appointmentType = AppointmentType.INDIVIDUAL)
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
        with(occurrences.single()) {
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
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
        with(occurrences.single()) {
          assertThat(internalLocationId).isEqualTo(request.internalLocationId)
          assertThat(inCell).isFalse
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
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
        with(occurrences.single()) {
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
      val request = AppointmentOccurrenceUpdateRequest(startDate = LocalDate.now().plusDays(3))

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.single()) {
          assertThat(startDate).isEqualTo(request.startDate)
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update start time success`() {
      val request = AppointmentOccurrenceUpdateRequest(startTime = LocalTime.of(13, 30))

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.single()) {
          assertThat(startTime).isEqualTo(request.startTime)
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update end time success`() {
      val request = AppointmentOccurrenceUpdateRequest(endTime = LocalTime.of(15, 0))

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.single()) {
          assertThat(endTime).isEqualTo(request.endTime)
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update comment success`() {
      val request = AppointmentOccurrenceUpdateRequest(comment = "Updated appointment occurrence level comment")

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(comment).isEqualTo("Appointment level comment")
        assertThat(appointmentDescription).isEqualTo("Appointment description")
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.single()) {
          assertThat(comment).isEqualTo(request.comment)
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update prisoner with no changes success`() {
      val request = AppointmentOccurrenceUpdateRequest(addPrisonerNumbers = emptyList())

      var index = 0
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!))
        .thenReturn(
          Mono.just(
            request.addPrisonerNumbers!!.map {
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

      verifyNoInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update all properties success`() {
      val request = AppointmentOccurrenceUpdateRequest(
        categoryCode = "NEW",
        internalLocationId = 456,
        startDate = LocalDate.now().plusDays(3),
        startTime = LocalTime.of(13, 30),
        endTime = LocalTime.of(15, 0),
        comment = "Updated appointment occurrence level comment",
        addPrisonerNumbers = emptyList(),
      )

      whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
        .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!, "New Category")))
      whenever(locationService.getLocationsForAppointmentsMap(appointment.prisonCode))
        .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, appointment.prisonCode)))
      var index = 0
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!))
        .thenReturn(
          Mono.just(
            request.addPrisonerNumbers!!.map {
              PrisonerSearchPrisonerFixture.instance(prisonerNumber = it, bookingId = 456L + index++, prisonId = appointment.prisonCode)
            },
          ),
        )

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(categoryCode).isEqualTo(request.categoryCode)
        assertThat(internalLocationId).isEqualTo(123)
        assertThat(inCell).isFalse
        assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
        assertThat(comment).isEqualTo("Appointment level comment")
        assertThat(appointmentDescription).isEqualTo("Appointment description")
        assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
        assertThat(updatedBy).isEqualTo("TEST.USER")
        with(occurrences.single()) {
          assertThat(internalLocationId).isEqualTo(request.internalLocationId)
          assertThat(inCell).isFalse
          assertThat(startDate).isEqualTo(request.startDate)
          assertThat(startTime).isEqualTo(request.startTime)
          assertThat(endTime).isEqualTo(request.endTime)
          assertThat(comment).isEqualTo(request.comment)
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

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update all properties`() {
      val request = AppointmentOccurrenceUpdateRequest(
        categoryCode = "NEW",
        internalLocationId = 456,
        startDate = LocalDate.now().plusDays(3),
        startTime = LocalTime.of(13, 30),
        endTime = LocalTime.of(15, 0),
        comment = "Updated appointment occurrence level comment",
      )

      whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
        .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!, "New Category")))
      whenever(locationService.getLocationsForAppointmentsMap(appointment.prisonCode))
        .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, appointment.prisonCode)))

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(categoryCode).isEqualTo(request.categoryCode)
        assertThat(internalLocationId).isEqualTo(123)
        assertThat(inCell).isFalse
        assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
        assertThat(comment).isEqualTo("Appointment level comment")
        assertThat(appointmentDescription).isEqualTo("Appointment description")
        assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
        assertThat(updatedBy).isEqualTo("TEST.USER")
        with(occurrences.single()) {
          assertThat(internalLocationId).isEqualTo(request.internalLocationId)
          assertThat(inCell).isFalse
          assertThat(startDate).isEqualTo(request.startDate)
          assertThat(startTime).isEqualTo(request.startTime)
          assertThat(endTime).isEqualTo(request.endTime)
          assertThat(comment).isEqualTo(request.comment)
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
          with(allocations.single()) {
            assertThat(prisonerNumber).isEqualTo("A1234BC")
            assertThat(bookingId).isEqualTo(456L)
          }
        }
      }

      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, appointmentOccurrenceAllocation.appointmentOccurrenceAllocationId)
      verifyNoMoreInteractions(outboundEventsService)
    }
  }

  @Nested
  @DisplayName("update group repeat appointment")
  inner class UpdateGroupRepeatAppointment {
    private val principal: Principal = mock()
    private val appointment = appointmentEntity(
      startDate = LocalDate.now().minusDays(3),
      updatedBy = null,
      prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 457),
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 4,
      appointmentType = AppointmentType.GROUP,
    )
    private val appointmentOccurrence = appointment.occurrences()[2]

    @BeforeEach
    fun setUp() {
      whenever(principal.name).thenReturn("TEST.USER")
      appointment.occurrences().forEach {
        whenever(appointmentOccurrenceRepository.findById(it.appointmentOccurrenceId)).thenReturn(
          Optional.of(it),
        )
      }

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
        with(occurrences) {
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
        }
      }

      appointment.occurrences().flatMap { it.allocations() }.forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(8.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update internal location id apply to this occurrence success`() {
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456, applyTo = ApplyTo.THIS_OCCURRENCE)

      whenever(locationService.getLocationsForAppointmentsMap(appointment.prisonCode))
        .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, appointment.prisonCode)))

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(internalLocationId).isEqualTo(123)
        assertThat(inCell).isFalse
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(123)
          assertThat(map { it.inCell }.distinct().single()).isFalse
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
        }
        with(occurrences[2]) {
          assertThat(internalLocationId).isEqualTo(request.internalLocationId)
          assertThat(inCell).isFalse
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
        with(occurrences[3]) {
          assertThat(internalLocationId).isEqualTo(123)
          assertThat(inCell).isFalse
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
        }
      }

      appointmentOccurrence.allocations().forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(2.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update internal location id apply to this and all future occurrences success`() {
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456, applyTo = ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES)

      whenever(locationService.getLocationsForAppointmentsMap(appointment.prisonCode))
        .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, appointment.prisonCode)))

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(internalLocationId).isEqualTo(123)
        assertThat(inCell).isFalse
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(123)
          assertThat(map { it.inCell }.distinct().single()).isFalse
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
        }
        with(occurrences.subList(2, response.occurrences.size)) {
          assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(request.internalLocationId)
          assertThat(map { it.inCell }.distinct().single()).isFalse
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
        }
      }

      appointment.occurrences().subList(2, appointment.occurrences().size).flatMap { it.allocations() }.forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_AND_ALL_FUTURE_OCCURRENCES")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(2.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(4.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update internal location id apply to all future occurrences success`() {
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456, applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)

      whenever(locationService.getLocationsForAppointmentsMap(appointment.prisonCode))
        .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, appointment.prisonCode)))

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(internalLocationId).isEqualTo(123)
        assertThat(inCell).isFalse
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences[0]) {
          assertThat(internalLocationId).isEqualTo(123)
          assertThat(inCell).isFalse
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
        }
        with(occurrences.subList(1, response.occurrences.size)) {
          assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(request.internalLocationId)
          assertThat(map { it.inCell }.distinct().single()).isFalse
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
        }
      }

      appointment.occurrences().subList(1, appointment.occurrences().size).flatMap { it.allocations() }.forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("ALL_FUTURE_OCCURRENCES")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(3.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(6.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update in cell = true apply to this occurrence success`() {
      val request = AppointmentOccurrenceUpdateRequest(inCell = true, applyTo = ApplyTo.THIS_OCCURRENCE)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(internalLocationId).isEqualTo(123)
        assertThat(inCell).isFalse
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(123)
          assertThat(map { it.inCell }.distinct().single()).isFalse
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
        }
        with(occurrences[2]) {
          assertThat(internalLocationId).isNull()
          assertThat(inCell).isTrue
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
        with(occurrences[3]) {
          assertThat(internalLocationId).isEqualTo(123)
          assertThat(inCell).isFalse
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
        }
      }

      appointmentOccurrence.allocations().forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(2.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update in cell = true apply to this and all future occurrences success`() {
      val request = AppointmentOccurrenceUpdateRequest(inCell = true, applyTo = ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(internalLocationId).isEqualTo(123)
        assertThat(inCell).isFalse
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(123)
          assertThat(map { it.inCell }.distinct().single()).isFalse
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
        }
        with(occurrences.subList(2, response.occurrences.size)) {
          assertThat(map { it.internalLocationId }.distinct().single()).isNull()
          assertThat(map { it.inCell }.distinct().single()).isTrue
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
        }
      }

      appointment.occurrences().subList(2, appointment.occurrences().size).flatMap { it.allocations() }.forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_AND_ALL_FUTURE_OCCURRENCES")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(2.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(4.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update in cell = true apply to all future occurrences success`() {
      val request = AppointmentOccurrenceUpdateRequest(inCell = true, applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(internalLocationId).isEqualTo(123)
        assertThat(inCell).isFalse
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences[0]) {
          assertThat(internalLocationId).isEqualTo(123)
          assertThat(inCell).isFalse
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
        }
        with(occurrences.subList(1, response.occurrences.size)) {
          assertThat(map { it.internalLocationId }.distinct().single()).isNull()
          assertThat(map { it.inCell }.distinct().single()).isTrue
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
        }
      }

      appointment.occurrences().subList(1, appointment.occurrences().size).flatMap { it.allocations() }.forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("ALL_FUTURE_OCCURRENCES")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(3.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(6.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update start date apply to this occurrence success`() {
      val request = AppointmentOccurrenceUpdateRequest(startDate = LocalDate.now().plusDays(1).plusWeeks(2), applyTo = ApplyTo.THIS_OCCURRENCE)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(startDate).isEqualTo(LocalDate.now().minusDays(3))
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        assertThat(response.occurrences[0].startDate).isEqualTo(LocalDate.now().minusDays(3))
        assertThat(response.occurrences[1].startDate).isEqualTo(LocalDate.now().minusDays(3).plusWeeks(1))
        assertThat(response.occurrences[2].startDate).isEqualTo(request.startDate)
        assertThat(response.occurrences[3].startDate).isEqualTo(LocalDate.now().minusDays(3).plusWeeks(3))
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
        }
        with(occurrences[2]) {
          assertThat(startDate).isEqualTo(request.startDate)
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
        with(occurrences[3]) {
          assertThat(startDate).isEqualTo(LocalDate.now().minusDays(3).plusWeeks(3))
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
        }
      }

      appointmentOccurrence.allocations().forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(2.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update start date apply to this and all future occurrences success`() {
      val request = AppointmentOccurrenceUpdateRequest(startDate = LocalDate.now().plusDays(1).plusWeeks(2), applyTo = ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(startDate).isEqualTo(LocalDate.now().minusDays(3))
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        assertThat(response.occurrences[0].startDate).isEqualTo(LocalDate.now().minusDays(3))
        assertThat(response.occurrences[1].startDate).isEqualTo(LocalDate.now().minusDays(3).plusWeeks(1))
        assertThat(response.occurrences[2].startDate).isEqualTo(request.startDate)
        assertThat(response.occurrences[3].startDate).isEqualTo(request.startDate!!.plusWeeks(1))
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
        }
        with(occurrences.subList(2, response.occurrences.size)) {
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
        }
      }

      appointment.occurrences().subList(2, appointment.occurrences().size).flatMap { it.allocations() }.forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_AND_ALL_FUTURE_OCCURRENCES")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(2.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(4.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update start time apply to this occurrence success`() {
      val request = AppointmentOccurrenceUpdateRequest(startTime = LocalTime.of(13, 30), applyTo = ApplyTo.THIS_OCCURRENCE)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.startTime }.distinct().single()).isEqualTo(LocalTime.of(9, 0))
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
        }
        with(occurrences[2]) {
          assertThat(startTime).isEqualTo(request.startTime)
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
        with(occurrences[3]) {
          assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
        }
      }

      appointmentOccurrence.allocations().forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(2.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update start time apply to this and all future occurrences success`() {
      val request = AppointmentOccurrenceUpdateRequest(startTime = LocalTime.of(13, 30), applyTo = ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.startTime }.distinct().single()).isEqualTo(LocalTime.of(9, 0))
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
        }
        with(occurrences.subList(2, response.occurrences.size)) {
          assertThat(map { it.startTime }.distinct().single()).isEqualTo(request.startTime)
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
        }
      }

      appointment.occurrences().subList(2, appointment.occurrences().size).flatMap { it.allocations() }.forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_AND_ALL_FUTURE_OCCURRENCES")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(2.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(4.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update start time apply to all future occurrences success`() {
      val request = AppointmentOccurrenceUpdateRequest(startTime = LocalTime.of(13, 30), applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences[0]) {
          assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
        }
        with(occurrences.subList(1, response.occurrences.size)) {
          assertThat(map { it.startTime }.distinct().single()).isEqualTo(request.startTime)
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
        }
      }

      appointment.occurrences().subList(1, appointment.occurrences().size).flatMap { it.allocations() }.forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("ALL_FUTURE_OCCURRENCES")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(3.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(6.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update end time apply to this occurrence success`() {
      val request = AppointmentOccurrenceUpdateRequest(endTime = LocalTime.of(15, 0), applyTo = ApplyTo.THIS_OCCURRENCE)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.endTime }.distinct().single()).isEqualTo(LocalTime.of(10, 30))
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
        }
        with(occurrences[2]) {
          assertThat(endTime).isEqualTo(request.endTime)
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
        with(occurrences[3]) {
          assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
        }
      }

      appointmentOccurrence.allocations().forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(2.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update end time apply to this and all future occurrences success`() {
      val request = AppointmentOccurrenceUpdateRequest(endTime = LocalTime.of(15, 0), applyTo = ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.endTime }.distinct().single()).isEqualTo(LocalTime.of(10, 30))
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
        }
        with(occurrences.subList(2, response.occurrences.size)) {
          assertThat(map { it.endTime }.distinct().single()).isEqualTo(request.endTime)
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
        }
      }

      appointment.occurrences().subList(2, appointment.occurrences().size).flatMap { it.allocations() }.forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_AND_ALL_FUTURE_OCCURRENCES")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(2.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(4.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update end time apply to all future occurrences success`() {
      val request = AppointmentOccurrenceUpdateRequest(endTime = LocalTime.of(15, 0), applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences[0]) {
          assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
        }
        with(occurrences.subList(1, response.occurrences.size)) {
          assertThat(map { it.endTime }.distinct().single()).isEqualTo(request.endTime)
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
        }
      }

      appointment.occurrences().subList(1, appointment.occurrences().size).flatMap { it.allocations() }.forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("ALL_FUTURE_OCCURRENCES")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(3.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(6.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update comment apply to this occurrence success`() {
      val request = AppointmentOccurrenceUpdateRequest(comment = "Updated appointment occurrence level comment", applyTo = ApplyTo.THIS_OCCURRENCE)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(comment).isEqualTo("Appointment level comment")
        assertThat(appointmentDescription).isEqualTo("Appointment description")
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.comment }.distinct().single()).isEqualTo("Appointment occurrence level comment")
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
        }
        with(occurrences[2]) {
          assertThat(comment).isEqualTo("Updated appointment occurrence level comment")
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
        }
        with(occurrences[3]) {
          assertThat(comment).isEqualTo("Appointment occurrence level comment")
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
        }
      }

      appointmentOccurrence.allocations().forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(2.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update comment apply to this and all future occurrences success`() {
      val request = AppointmentOccurrenceUpdateRequest(comment = "Updated appointment occurrence level comment", applyTo = ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(comment).isEqualTo("Appointment level comment")
        assertThat(appointmentDescription).isEqualTo("Appointment description")
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.comment }.distinct().single()).isEqualTo("Appointment occurrence level comment")
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
        }
        with(occurrences.subList(2, response.occurrences.size)) {
          assertThat(map { it.comment }.distinct().single()).isEqualTo("Updated appointment occurrence level comment")
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
        }
      }

      appointment.occurrences().subList(2, appointment.occurrences().size).flatMap { it.allocations() }.forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_AND_ALL_FUTURE_OCCURRENCES")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(2.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(4.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update comment apply to all future occurrences success`() {
      val request = AppointmentOccurrenceUpdateRequest(comment = "Updated appointment occurrence level comment", applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(comment).isEqualTo("Appointment level comment")
        assertThat(appointmentDescription).isEqualTo("Appointment description")
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences[0]) {
          assertThat(comment).isEqualTo("Appointment occurrence level comment")
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
        }
        with(occurrences.subList(1, response.occurrences.size)) {
          assertThat(map { it.comment }.distinct().single()).isEqualTo("Updated appointment occurrence level comment")
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
        }
      }

      appointment.occurrences().subList(1, appointment.occurrences().size).flatMap { it.allocations() }.forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("ALL_FUTURE_OCCURRENCES")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(3.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(6.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update prisoner list remove one prisoner add one prisoner apply to this occurrence success`() {
      val request = AppointmentOccurrenceUpdateRequest(
        addPrisonerNumbers = listOf("B2345CD", "C3456DE"),
        removePrisonerNumbers = listOf("A1234BC"),
        applyTo = ApplyTo.THIS_OCCURRENCE,
      )

      var index = 0
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!))
        .thenReturn(
          Mono.just(
            request.addPrisonerNumbers!!.map {
              PrisonerSearchPrisonerFixture.instance(prisonerNumber = it, bookingId = 457L + index++, prisonId = appointment.prisonCode)
            },
          ),
        )

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
          assertThat(map { it.allocations[0].prisonerNumber }.distinct().single()).isEqualTo("A1234BC")
          assertThat(map { it.allocations[0].bookingId }.distinct().single()).isEqualTo(456)
          assertThat(map { it.allocations[1].prisonerNumber }.distinct().single()).isEqualTo("B2345CD")
          assertThat(map { it.allocations[1].bookingId }.distinct().single()).isEqualTo(457)
        }
        with(occurrences[2]) {
          assertThat(updated).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(updatedBy).isEqualTo("TEST.USER")
          assertThat(allocations[0].prisonerNumber).isEqualTo("B2345CD")
          assertThat(allocations[0].bookingId).isEqualTo(457)
          assertThat(allocations[1].prisonerNumber).isEqualTo("C3456DE")
          assertThat(allocations[1].bookingId).isEqualTo(458)
        }
        with(occurrences[3]) {
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
          assertThat(allocations[0].prisonerNumber).isEqualTo("A1234BC")
          assertThat(allocations[0].bookingId).isEqualTo(456)
          assertThat(allocations[1].prisonerNumber).isEqualTo("B2345CD")
          assertThat(allocations[1].bookingId).isEqualTo(457)
        }
      }

      verifyNoInteractions(outboundEventsService)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_EDITED.name),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_NAME_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("3")
        assertThat(value[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(2.0)
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(3.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }

    @Test
    fun `update prisoner list remove one prisoner add one prisoner apply to this and all future occurrences success`() {
      val request = AppointmentOccurrenceUpdateRequest(
        addPrisonerNumbers = listOf("B2345CD", "C3456DE"),
        removePrisonerNumbers = listOf("A1234BC"),
        applyTo = ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES,
      )

      var index = 0
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!))
        .thenReturn(
          Mono.just(
            request.addPrisonerNumbers!!.map {
              PrisonerSearchPrisonerFixture.instance(prisonerNumber = it, bookingId = 457L + index++, prisonId = appointment.prisonCode)
            },
          ),
        )

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
          assertThat(map { it.allocations[0].prisonerNumber }.distinct().single()).isEqualTo("A1234BC")
          assertThat(map { it.allocations[0].bookingId }.distinct().single()).isEqualTo(456)
          assertThat(map { it.allocations[1].prisonerNumber }.distinct().single()).isEqualTo("B2345CD")
          assertThat(map { it.allocations[1].bookingId }.distinct().single()).isEqualTo(457)
        }
        with(occurrences.subList(2, response.occurrences.size)) {
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
          assertThat(map { it.allocations[0].prisonerNumber }.distinct().single()).isEqualTo("B2345CD")
          assertThat(map { it.allocations[0].bookingId }.distinct().single()).isEqualTo(457)
          assertThat(map { it.allocations[1].prisonerNumber }.distinct().single()).isEqualTo("C3456DE")
          assertThat(map { it.allocations[1].bookingId }.distinct().single()).isEqualTo(458)
        }
      }

      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update prisoner list remove one prisoner add one prisoner apply to all future occurrences success success`() {
      val request = AppointmentOccurrenceUpdateRequest(
        addPrisonerNumbers = listOf("B2345CD", "C3456DE"),
        removePrisonerNumbers = listOf("A1234BC"),
        applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES,
      )

      var index = 0
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!))
        .thenReturn(
          Mono.just(
            request.addPrisonerNumbers!!.map {
              PrisonerSearchPrisonerFixture.instance(prisonerNumber = it, bookingId = 457L + index++, prisonId = appointment.prisonCode)
            },
          ),
        )

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences[0]) {
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
          assertThat(allocations[0].prisonerNumber).isEqualTo("A1234BC")
          assertThat(allocations[0].bookingId).isEqualTo(456)
          assertThat(allocations[1].prisonerNumber).isEqualTo("B2345CD")
          assertThat(allocations[1].bookingId).isEqualTo(457)
        }
        with(occurrences.subList(1, response.occurrences.size)) {
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
          assertThat(map { it.allocations[0].prisonerNumber }.distinct().single()).isEqualTo("B2345CD")
          assertThat(map { it.allocations[0].bookingId }.distinct().single()).isEqualTo(457)
          assertThat(map { it.allocations[1].prisonerNumber }.distinct().single()).isEqualTo("C3456DE")
          assertThat(map { it.allocations[1].bookingId }.distinct().single()).isEqualTo(458)
        }
      }

      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update all properties apply to this and all future occurrences success`() {
      val request = AppointmentOccurrenceUpdateRequest(
        categoryCode = "NEW",
        internalLocationId = 456,
        startDate = LocalDate.now().plusDays(3),
        startTime = LocalTime.of(13, 30),
        endTime = LocalTime.of(15, 0),
        comment = "Updated appointment occurrence level comment",
        addPrisonerNumbers = listOf("B2345CD", "C3456DE"),
        removePrisonerNumbers = listOf("A1234BC"),
        applyTo = ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES,
      )

      whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
        .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!, "New Category")))
      whenever(locationService.getLocationsForAppointmentsMap(appointment.prisonCode))
        .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, appointment.prisonCode)))
      var index = 0
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!))
        .thenReturn(
          Mono.just(
            request.addPrisonerNumbers!!.map {
              PrisonerSearchPrisonerFixture.instance(prisonerNumber = it, bookingId = 457L + index++, prisonId = appointment.prisonCode)
            },
          ),
        )

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(categoryCode).isEqualTo(request.categoryCode)
        assertThat(internalLocationId).isEqualTo(123)
        assertThat(inCell).isFalse
        assertThat(startDate).isEqualTo(LocalDate.now().minusDays(3))
        assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
        assertThat(comment).isEqualTo("Appointment level comment")
        assertThat(appointmentDescription).isEqualTo("Appointment description")
        assertThat(updated).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(updatedBy).isEqualTo("TEST.USER")
        assertThat(occurrences[0].startDate).isEqualTo(LocalDate.now().minusDays(3))
        assertThat(occurrences[1].startDate).isEqualTo(LocalDate.now().minusDays(3).plusWeeks(1))
        assertThat(occurrences[2].startDate).isEqualTo(request.startDate)
        assertThat(occurrences[3].startDate).isEqualTo(request.startDate!!.plusWeeks(1))
        with(occurrences.subList(0, 2)) {
          assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(123)
          assertThat(map { it.inCell }.distinct().single()).isFalse
          assertThat(map { it.startTime }.distinct().single()).isEqualTo(LocalTime.of(9, 0))
          assertThat(map { it.endTime }.distinct().single()).isEqualTo(LocalTime.of(10, 30))
          assertThat(map { it.comment }.distinct().single()).isEqualTo("Appointment occurrence level comment")
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
          assertThat(map { it.allocations[0].prisonerNumber }.distinct().single()).isEqualTo("A1234BC")
          assertThat(map { it.allocations[0].bookingId }.distinct().single()).isEqualTo(456)
          assertThat(map { it.allocations[1].prisonerNumber }.distinct().single()).isEqualTo("B2345CD")
          assertThat(map { it.allocations[1].bookingId }.distinct().single()).isEqualTo(457)
        }
        with(occurrences.subList(2, occurrences.size)) {
          assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(request.internalLocationId)
          assertThat(map { it.inCell }.distinct().single()).isFalse
          assertThat(map { it.startTime }.distinct().single()).isEqualTo(request.startTime)
          assertThat(map { it.endTime }.distinct().single()).isEqualTo(request.endTime)
          assertThat(map { it.comment }.distinct().single()).isEqualTo("Updated appointment occurrence level comment")
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
          assertThat(map { it.allocations[0].prisonerNumber }.distinct().single()).isEqualTo("B2345CD")
          assertThat(map { it.allocations[0].bookingId }.distinct().single()).isEqualTo(457)
          assertThat(map { it.allocations[1].prisonerNumber }.distinct().single()).isEqualTo("C3456DE")
          assertThat(map { it.allocations[1].bookingId }.distinct().single()).isEqualTo(458)
        }
      }

      appointment.occurrences().subList(0, 2).flatMap { it.allocations() }
        .union(appointment.occurrences().subList(2, response.occurrences.size).map { it.allocations()[0] }).forEach {
          verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
        }
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `update should filter out cancelled occurrences`() {
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456, applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)
      appointment.occurrences()[1].cancellationReason = AppointmentCancellationReason(2L, "Cancelled", false)

      whenever(locationService.getLocationsForAppointmentsMap(appointment.prisonCode))
        .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, appointment.prisonCode)))

      val response = service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

      with(response) {
        assertThat(internalLocationId).isEqualTo(123)
        assertThat(inCell).isFalse
        assertThat(updated).isNull()
        assertThat(updatedBy).isNull()
        with(occurrences.subList(0, 1)) {
          assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(123)
          assertThat(map { it.inCell }.distinct().single()).isFalse
          assertThat(map { it.updated }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
        }
        with(occurrences.subList(2, response.occurrences.size)) {
          assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(request.internalLocationId)
          assertThat(map { it.inCell }.distinct().single()).isFalse
          assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
        }
      }

      appointment.occurrences().subList(2, appointment.occurrences().size).flatMap { it.allocations() }.forEach {
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
      }
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `update appointment throws caseload access exception if caseload id header does not match`() {
      addCaseloadIdToRequestHeader("WRONG")
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456, applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)
      assertThatThrownBy { service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal) }
        .isInstanceOf(CaseloadAccessException::class.java)
    }
  }
}
