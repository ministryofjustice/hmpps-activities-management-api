package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancellationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CancelAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UncancelAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UpdateAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUncancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventOrganiserRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLY_TO_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Optional

class AppointmentServiceUncancelTest {
  private val appointmentSeriesRepository: AppointmentSeriesRepository = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentAttendeeRemovalReasonRepository: AppointmentAttendeeRemovalReasonRepository = mock()
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository = mock()
  private val eventTierRepository: EventTierRepository = mock()
  private val eventOrganiserRepository: EventOrganiserRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val updateAppointmentsJob: UpdateAppointmentsJob = mock()
  private val cancelAppointmentsJob: CancelAppointmentsJob = mock()
  private val uncancelAppointmentsJob: UncancelAppointmentsJob = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val auditService: AuditService = mock()
  private val telemetryClient: TelemetryClient = mock()

  @Captor
  private lateinit var telemetryPropertyMap: ArgumentCaptor<Map<String, String>>

  @Captor
  private lateinit var telemetryMetricsMap: ArgumentCaptor<Map<String, Double>>

  private val service = AppointmentService(
    appointmentRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    prisonApiClient,
    AppointmentUpdateDomainService(
      appointmentSeriesRepository,
      appointmentAttendeeRemovalReasonRepository,
      eventTierRepository,
      eventOrganiserRepository,
      TransactionHandler(),
      outboundEventsService,
      telemetryClient,
      auditService,
    ),
    AppointmentCancelDomainService(
      appointmentSeriesRepository,
      appointmentCancellationReasonRepository,
      telemetryClient,
      auditService,
      TransactionHandler(),
      outboundEventsService,
    ),
    updateAppointmentsJob,
    cancelAppointmentsJob,
    uncancelAppointmentsJob,
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
  @DisplayName("uncancel individual appointment validation")
  inner class UncancelIndividualAppointmentValidation {

    private val principal: Principal = mock()
    private val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now().plusDays(1), updatedBy = null)
    private val appointment = appointmentSeries.appointments().first()
    private val existingCancelledReason = AppointmentCancellationReason(1L, "Cancelled", false)

    @BeforeEach
    fun setUp() {
      whenever(principal.name).thenReturn("TEST.USER")
      whenever(appointmentCancellationReasonRepository.findById(existingCancelledReason.appointmentCancellationReasonId)).thenReturn(
        Optional.of(existingCancelledReason),
      )
      whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
        Optional.of(appointment),
      )
    }

    @Test
    fun `uncancel appointment throws illegal argument exception when appointment is not cancelled`() {
      val request = AppointmentUncancelRequest(ApplyTo.THIS_APPOINTMENT)

      val appointmentSeries = appointmentSeriesEntity(
        appointmentSeriesId = 2,
        startDate = LocalDate.now(),
        startTime = LocalTime.now(),
        endTime = LocalTime.now().plusHours(1),
      )
      val appointment = appointmentSeries.appointments().first()

      whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
        Optional.of(appointment),
      )

      assertThatThrownBy {
        service.uncancelAppointment(
          appointment.appointmentId,
          request,
          principal,
        )
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot uncancel a not cancelled appointment")

      verify(appointmentSeriesRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `uncancel appointment throws illegal argument exception when appointment is more than five days old`() {
      val request = AppointmentUncancelRequest(ApplyTo.THIS_APPOINTMENT)

      val appointmentSeries = appointmentSeriesEntity(
        appointmentSeriesId = 2,
        startDate = LocalDate.now().minusDays(6),
        startTime = LocalTime.now().minusMinutes(1),
        endTime = LocalTime.now().plusHours(1),
      )
      val appointment = appointmentSeries.appointments().first()

      whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
        Optional.of(appointment),
      )

      assertThatThrownBy {
        service.uncancelAppointment(
          appointment.appointmentId,
          request,
          principal,
        )
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot uncancel an appointment more than 5 days ago")

      verify(appointmentSeriesRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `uncancel appointment throws entity not found exception for unknown appointment id`() {
      val request = AppointmentUncancelRequest(ApplyTo.THIS_APPOINTMENT)

      assertThatThrownBy { service.uncancelAppointment(-1, request, mock()) }.isInstanceOf(
        EntityNotFoundException::class.java,
      )
        .hasMessage("Appointment -1 not found")

      verify(appointmentSeriesRepository, never()).saveAndFlush(any())
    }

    @Nested
    @DisplayName("uncancel individual appointment")
    @ExtendWith(FakeSecurityContext::class)
    inner class UncancelIndividualAppointment {

      private val principal: Principal = mock()
      private val appointmentSeries = appointmentSeriesEntity(
        startDate = LocalDate.now().plusDays(1),
        updatedBy = null,
        cancelledBy = "CANCEL.USER",
        cancelledTime = LocalDateTime.now().plusDays(1),
        cancellationReason = existingCancelledReason,
      )

      private val appointment = appointmentSeries.appointments().first()

      @BeforeEach
      fun setUp() {
        whenever(principal.name).thenReturn("TEST.USER")
        whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
          Optional.of(appointment),
        )
        whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeries)).thenReturn(appointmentSeries)
      }

      @Test
      fun `uncancel appointment sets an appointment to not be cancelled`() {
        val request = AppointmentUncancelRequest(ApplyTo.THIS_APPOINTMENT)

        service.uncancelAppointment(appointment.appointmentId, request, principal)

        with(appointment) {
          assertThat(cancelledTime).isNull()
          assertThat(cancelledBy).isNull()
          assertThat(cancellationReason).isNull()
          assertThat(updatedBy).isEqualTo("TEST.USER")
          assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        }

        appointment.attendees().forEach {
          verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UNCANCELLED, it.appointmentAttendeeId)
        }
        verifyNoMoreInteractions(outboundEventsService)

        verify(telemetryClient).trackEvent(
          eq(TelemetryEvent.APPOINTMENT_UNCANCELLED.value),
          telemetryPropertyMap.capture(),
          telemetryMetricsMap.capture(),
        )

        with(telemetryPropertyMap) {
          assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
          assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
          assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
          assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("1")
          assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_APPOINTMENT")
        }

        with(telemetryMetricsMap) {
          assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
          assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(1.0)
          assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull
        }
      }
    }

    @Nested
    @DisplayName("uncancel group repeat appointment")
    @ExtendWith(FakeSecurityContext::class)
    inner class UncancelGroupRepeatAppointment {

      private val principal: Principal = mock()
      private val appointmentSeries = appointmentSeriesEntity(
        startDate = LocalDate.now().minusDays(3),
        updatedBy = null,
        prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 457),
        frequency = AppointmentFrequency.WEEKLY,
        numberOfAppointments = 4,
        cancelledBy = "CANCEL.USER",
        cancelledTime = LocalDateTime.now().plusDays(1),
        cancellationReason = existingCancelledReason,
      )
      private val appointments = appointmentSeries.appointments()
      private val appointment = appointments[2]

      @BeforeEach
      fun setUp() {
        whenever(principal.name).thenReturn("TEST.USER")
        appointmentSeries.appointments().forEach {
          whenever(appointmentRepository.findById(it.appointmentId)).thenReturn(
            Optional.of(it),
          )
        }
        whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeries)).thenReturn(appointmentSeries)
      }

      @Test
      fun `uncancel appointment that applies to this appointment only`() {
        val request = AppointmentUncancelRequest(ApplyTo.THIS_APPOINTMENT)
        service.uncancelAppointment(appointment.appointmentId, request, principal)

        with(appointments.subList(0, 2)) {
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
          assertThat(map { it.updatedTime }.distinct().single()).isNull()
        }
        with(appointments[2]) {
          assertThat(cancellationReason?.appointmentCancellationReasonId).isNull()
          assertThat(updatedBy).isEqualTo("TEST.USER")
          assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        }
        with(appointments[3]) {
          assertThat(updatedBy).isNull()
          assertThat(updatedTime).isNull()
        }
      }

      @Test
      fun `uncancel appointment that applies to this and all future appointments`() {
        val request = AppointmentUncancelRequest(ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS)

        service.uncancelAppointment(appointment.appointmentId, request, principal)

        with(appointments.subList(0, 2)) {
          assertThat(map { it.updatedBy }.distinct().single()).isNull()
          assertThat(map { it.updatedTime }.distinct().single()).isNull()
          assertThat(map { it.cancellationReason?.appointmentCancellationReasonId }.distinct().single()).isEqualTo(1L)
          assertThat(map { it.cancelledBy }.distinct().single()).isEqualTo("CANCEL.USER")
          assertThat(map { it.cancelledTime }.distinct().single()).isNotNull()
        }
        with(appointments.subList(2, appointments.size)) {
          assertThat(map { it.cancellationReason?.appointmentCancellationReasonId }.distinct().single()).isNull()
          assertThat(map { it.cancelledTime }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
          assertThat(map { it.updatedTime }.distinct().single()).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `uncancel appointment with a reason that applies to all future appointments`() {
        val request = AppointmentUncancelRequest(ApplyTo.ALL_FUTURE_APPOINTMENTS)

        service.uncancelAppointment(appointment.appointmentId, request, principal)

        with(appointments[0]) {
          assertThat(updatedBy).isNull()
          assertThat(updatedTime).isNull()
          assertThat(cancellationReason?.appointmentCancellationReasonId).isEqualTo(1L)
          assertThat(cancelledBy).isEqualTo("CANCEL.USER")
          assertThat(cancelledTime).isNotNull()
        }

        with(appointments.subList(1, appointments.size)) {
          assertThat(map { it.cancellationReason?.appointmentCancellationReasonId }.distinct().single()).isNull()
          assertThat(map { it.cancelledBy }.distinct().single()).isNull()
          assertThat(map { it.cancelledTime }.distinct().single()).isNull()
          assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("TEST.USER")
          assertThat(map { it.updatedTime }.distinct().single()).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `uncancel appointment throws caseload access exception if caseload id header does not match`() {
        addCaseloadIdToRequestHeader("WRONG")
        val request = AppointmentUncancelRequest(ApplyTo.ALL_FUTURE_APPOINTMENTS)
        assertThatThrownBy { service.uncancelAppointment(appointment.appointmentId, request, principal) }
          .isInstanceOf(CaseloadAccessException::class.java)
      }
    }
  }
}
