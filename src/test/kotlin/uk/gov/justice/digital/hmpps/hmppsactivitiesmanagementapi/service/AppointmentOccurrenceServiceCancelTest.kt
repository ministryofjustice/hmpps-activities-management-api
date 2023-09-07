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
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancellationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CancelAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UpdateAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
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

class AppointmentOccurrenceServiceCancelTest {
  private val appointmentSeriesRepository: AppointmentSeriesRepository = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val updateAppointmentsJob: UpdateAppointmentsJob = mock()
  private val cancelAppointmentsJob: CancelAppointmentsJob = mock()
  private val auditService: AuditService = mock()
  private val telemetryClient: TelemetryClient = mock()

  @Captor
  private lateinit var telemetryPropertyMap: ArgumentCaptor<Map<String, String>>

  @Captor
  private lateinit var telemetryMetricsMap: ArgumentCaptor<Map<String, Double>>

  private val service = AppointmentOccurrenceService(
    appointmentRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    AppointmentUpdateDomainService(appointmentSeriesRepository, telemetryClient, auditService),
    AppointmentCancelDomainService(appointmentSeriesRepository, appointmentCancellationReasonRepository, telemetryClient, auditService),
    updateAppointmentsJob,
    cancelAppointmentsJob,
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
  @DisplayName("cancel individual appointment validation")
  inner class CancelIndividualAppointmentValidation {

    private val principal: Principal = mock()
    private val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now().plusDays(1), updatedBy = null)
    private val appointmentOccurrence = appointmentSeries.appointments().first()
    private val cancellationReason = AppointmentCancellationReason(1L, "Cancelled", false)

    @BeforeEach
    fun setUp() {
      whenever(principal.name).thenReturn("TEST.USER")
      whenever(appointmentCancellationReasonRepository.findById(cancellationReason.appointmentCancellationReasonId)).thenReturn(
        Optional.of(cancellationReason),
      )
      whenever(appointmentRepository.findById(appointmentOccurrence.appointmentId)).thenReturn(
        Optional.of(appointmentOccurrence),
      )
    }

    @Test
    fun `cancel appointment throws illegal argument exception when appointment occurrence is in the past`() {
      val request = AppointmentOccurrenceCancelRequest(1, ApplyTo.THIS_OCCURRENCE)

      val appointmentSeries = appointmentSeriesEntity(
        appointmentSeriesId = 2,
        startDate = LocalDate.now(),
        startTime = LocalTime.now().minusMinutes(1),
        endTime = LocalTime.now().plusHours(1),
      )
      val appointmentOccurrence = appointmentSeries.appointments().first()

      whenever(appointmentRepository.findById(appointmentOccurrence.appointmentId)).thenReturn(
        Optional.of(appointmentOccurrence),
      )

      assertThatThrownBy {
        service.cancelAppointmentOccurrence(
          appointmentOccurrence.appointmentId,
          request,
          principal,
        )
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot cancel a past appointment")

      verify(appointmentSeriesRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `cancel appointment throws entity not found exception for unknown appointment occurrence id`() {
      val request = AppointmentOccurrenceCancelRequest(1, ApplyTo.THIS_OCCURRENCE)

      assertThatThrownBy { service.cancelAppointmentOccurrence(-1, request, mock()) }.isInstanceOf(
        EntityNotFoundException::class.java,
      )
        .hasMessage("Appointment -1 not found")

      verify(appointmentSeriesRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `cancel appointment throws entity not found exception for unknown appointment cancellation reason id`() {
      val request = AppointmentOccurrenceCancelRequest(-1, ApplyTo.THIS_OCCURRENCE)

      whenever(appointmentRepository.findById(appointmentOccurrence.appointmentId)).thenReturn(
        Optional.of(appointmentOccurrence),
      )

      assertThatThrownBy {
        service.cancelAppointmentOccurrence(
          appointmentOccurrence.appointmentId,
          request,
          principal,
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Appointment Cancellation Reason -1 not found")

      verify(appointmentSeriesRepository, never()).saveAndFlush(any())
    }
  }

  @Nested
  @DisplayName("cancel individual appointment")
  @ExtendWith(FakeSecurityContext::class)
  inner class CancelIndividualAppointment {

    private val principal: Principal = mock()
    private val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now().plusDays(1), updatedBy = null)
    private val appointmentOccurrence = appointmentSeries.appointments().first()
    private val softDeleteCancellationReason = AppointmentCancellationReason(1L, "Created in error", true)
    private val cancellationReason = AppointmentCancellationReason(2L, "Cancelled", false)

    @BeforeEach
    fun setUp() {
      whenever(principal.name).thenReturn("TEST.USER")
      whenever(appointmentCancellationReasonRepository.findById(softDeleteCancellationReason.appointmentCancellationReasonId)).thenReturn(
        Optional.of(softDeleteCancellationReason),
      )
      whenever(appointmentCancellationReasonRepository.findById(cancellationReason.appointmentCancellationReasonId)).thenReturn(
        Optional.of(cancellationReason),
      )
      whenever(appointmentRepository.findById(appointmentOccurrence.appointmentId)).thenReturn(
        Optional.of(appointmentOccurrence),
      )
      whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeries)).thenReturn(appointmentSeries)
    }

    @Test
    fun `cancel appointment with reason that triggers soft delete success`() {
      val request = AppointmentOccurrenceCancelRequest(1, ApplyTo.THIS_OCCURRENCE)

      service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal)

      with(appointmentOccurrence) {
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(cancelledBy).isEqualTo("TEST.USER")
        assertThat(cancellationReason?.appointmentCancellationReasonId).isEqualTo(1L)
        assertThat(isDeleted).isTrue
      }

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_DELETED.value),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull
      }
    }

    @Test
    fun `cancel appointment with reason that does not trigger soft delete success`() {
      val request = AppointmentOccurrenceCancelRequest(2, ApplyTo.THIS_OCCURRENCE)

      service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal)

      with(appointmentOccurrence) {
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(cancelledBy).isEqualTo("TEST.USER")
        assertThat(cancellationReason?.appointmentCancellationReasonId).isEqualTo(2L)
        assertThat(isDeleted).isFalse
      }

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_CANCELLED.value),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[APPLY_TO_PROPERTY_KEY]).isEqualTo("THIS_OCCURRENCE")
      }

      with(telemetryMetricsMap) {
        assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull
      }
    }
  }

  @Nested
  @DisplayName("cancel group repeat appointment")
  @ExtendWith(FakeSecurityContext::class)
  inner class CancelGroupRepeatAppointment {

    private val principal: Principal = mock()
    private val appointmentSeries = appointmentSeriesEntity(
      startDate = LocalDate.now().minusDays(3),
      updatedBy = null,
      prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 457),
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 4,
    )
    private val appointmentOccurrences = appointmentSeries.appointments()
    private val appointmentOccurrence = appointmentOccurrences[2]
    private val softDeleteCancellationReason = AppointmentCancellationReason(1L, "Created in error", true)
    private val cancellationReason = AppointmentCancellationReason(2L, "Cancelled", false)

    @BeforeEach
    fun setUp() {
      whenever(principal.name).thenReturn("TEST.USER")
      appointmentSeries.appointments().forEach {
        whenever(appointmentRepository.findById(it.appointmentId)).thenReturn(
          Optional.of(it),
        )
      }
      whenever(appointmentCancellationReasonRepository.findById(softDeleteCancellationReason.appointmentCancellationReasonId)).thenReturn(
        Optional.of(softDeleteCancellationReason),
      )
      whenever(appointmentCancellationReasonRepository.findById(cancellationReason.appointmentCancellationReasonId)).thenReturn(
        Optional.of(cancellationReason),
      )
      whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeries)).thenReturn(appointmentSeries)
    }

    @Test
    fun `cancel appointment with a reason that triggers a soft delete and that applies to this occurrence only`() {
      val request = AppointmentOccurrenceCancelRequest(1, ApplyTo.THIS_OCCURRENCE)
      service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal)

      with(appointmentOccurrences.subList(0, 2)) {
        assertThat(map { it.cancellationReason?.appointmentCancellationReasonId }.distinct().single()).isNull()
        assertThat(map { it.cancelledTime }.distinct().single()).isNull()
        assertThat(map { it.cancelledBy }.distinct().single()).isNull()
        assertThat(map { it.isDeleted }.distinct().single()).isFalse
      }
      with(appointmentOccurrences[2]) {
        assertThat(cancellationReason?.appointmentCancellationReasonId).isEqualTo(1)
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(cancelledBy).isEqualTo("TEST.USER")
        assertThat(isDeleted).isTrue
      }
      with(appointmentOccurrences[3]) {
        assertThat(cancellationReason?.appointmentCancellationReasonId).isNull()
        assertThat(cancelledTime).isNull()
        assertThat(cancelledBy).isNull()
        assertThat(isDeleted).isFalse
      }
    }

    @Test
    fun `cancel appointment with a reason that does not trigger a soft delete and that applies to this occurrence only`() {
      val request = AppointmentOccurrenceCancelRequest(2, ApplyTo.THIS_OCCURRENCE)
      service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal)

      with(appointmentOccurrences.subList(0, 2)) {
        assertThat(map { it.cancellationReason?.appointmentCancellationReasonId }.distinct().single()).isNull()
        assertThat(map { it.cancelledTime }.distinct().single()).isNull()
        assertThat(map { it.cancelledBy }.distinct().single()).isNull()
        assertThat(map { it.isDeleted }.distinct().single()).isFalse
      }
      with(appointmentOccurrences[2]) {
        assertThat(cancellationReason?.appointmentCancellationReasonId).isEqualTo(2)
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(cancelledBy).isEqualTo("TEST.USER")
        assertThat(isDeleted).isFalse
      }
      with(appointmentOccurrences[3]) {
        assertThat(cancellationReason?.appointmentCancellationReasonId).isNull()
        assertThat(cancelledTime).isNull()
        assertThat(cancelledBy).isNull()
        assertThat(isDeleted).isFalse
      }
    }

    @Test
    fun `cancel appointment with a reason that triggers a soft delete and that applies to this and all future occurrences`() {
      val request = AppointmentOccurrenceCancelRequest(1, ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES)

      service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal)

      with(appointmentOccurrences.subList(0, 2)) {
        assertThat(map { it.cancellationReason?.appointmentCancellationReasonId }.distinct().single()).isNull()
        assertThat(map { it.cancelledTime }.distinct().single()).isNull()
        assertThat(map { it.cancelledBy }.distinct().single()).isNull()
        assertThat(map { it.isDeleted }.distinct().single()).isFalse
      }
      with(appointmentOccurrences.subList(2, appointmentOccurrences.size)) {
        assertThat(map { it.cancellationReason?.appointmentCancellationReasonId }.distinct().single()).isEqualTo(1)
        assertThat(map { it.cancelledTime }.distinct().single()).isCloseTo(
          LocalDateTime.now(),
          within(60, ChronoUnit.SECONDS),
        )
        assertThat(map { it.cancelledBy }.distinct().single()).isEqualTo("TEST.USER")
        assertThat(map { it.isDeleted }.distinct().single()).isTrue
      }
    }

    @Test
    fun `cancel appointment with a reason that does not trigger a soft delete and that applies to this and all future occurrences`() {
      val request = AppointmentOccurrenceCancelRequest(2, ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES)

      service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal)

      with(appointmentOccurrences.subList(0, 2)) {
        assertThat(map { it.cancellationReason?.appointmentCancellationReasonId }.distinct().single()).isNull()
        assertThat(map { it.cancelledTime }.distinct().single()).isNull()
        assertThat(map { it.cancelledBy }.distinct().single()).isNull()
        assertThat(map { it.isDeleted }.distinct().single()).isFalse
      }
      with(appointmentOccurrences.subList(2, appointmentOccurrences.size)) {
        assertThat(map { it.cancellationReason?.appointmentCancellationReasonId }.distinct().single()).isEqualTo(2)
        assertThat(map { it.cancelledTime }.distinct().single()).isCloseTo(
          LocalDateTime.now(),
          within(60, ChronoUnit.SECONDS),
        )
        assertThat(map { it.cancelledBy }.distinct().single()).isEqualTo("TEST.USER")
        assertThat(map { it.isDeleted }.distinct().single()).isFalse
      }
    }

    @Test
    fun `cancel appointment with a reason that triggers a soft delete and that applies to all future occurrences`() {
      val request = AppointmentOccurrenceCancelRequest(1, ApplyTo.ALL_FUTURE_OCCURRENCES)

      service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal)

      with(appointmentOccurrences[0]) {
        assertThat(cancellationReason?.appointmentCancellationReasonId).isNull()
        assertThat(cancelledBy).isNull()
        assertThat(cancelledTime).isNull()
      }

      with(appointmentOccurrences.subList(1, appointmentOccurrences.size)) {
        assertThat(map { it.cancellationReason?.appointmentCancellationReasonId }.distinct().single()).isEqualTo(1)
        assertThat(map { it.cancelledTime }.distinct().single()).isCloseTo(
          LocalDateTime.now(),
          within(60, ChronoUnit.SECONDS),
        )
        assertThat(map { it.cancelledBy }.distinct().single()).isEqualTo("TEST.USER")
        assertThat(map { it.isDeleted }.distinct().single()).isTrue
      }
    }

    @Test
    fun `cancel appointment with a reason that does not trigger a soft delete and that applies to all future occurrences`() {
      val request = AppointmentOccurrenceCancelRequest(2, ApplyTo.ALL_FUTURE_OCCURRENCES)

      service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal)

      with(appointmentOccurrences[0]) {
        assertThat(cancellationReason?.appointmentCancellationReasonId).isNull()
        assertThat(cancelledBy).isNull()
        assertThat(cancelledTime).isNull()
      }

      with(appointmentOccurrences.subList(1, appointmentOccurrences.size)) {
        assertThat(map { it.cancellationReason?.appointmentCancellationReasonId }.distinct().single()).isEqualTo(2)
        assertThat(map { it.cancelledTime }.distinct().single()).isCloseTo(
          LocalDateTime.now(),
          within(60, ChronoUnit.SECONDS),
        )
        assertThat(map { it.cancelledBy }.distinct().single()).isEqualTo("TEST.USER")
        assertThat(map { it.isDeleted }.distinct().single()).isFalse
      }
    }

    @Test
    fun `cancel appointment throws caseload access exception if caseload id header does not match`() {
      addCaseloadIdToRequestHeader("WRONG")
      val request = AppointmentOccurrenceCancelRequest(2, ApplyTo.ALL_FUTURE_OCCURRENCES)
      assertThatThrownBy { service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentId, request, principal) }
        .isInstanceOf(CaseloadAccessException::class.java)
    }
  }
}
