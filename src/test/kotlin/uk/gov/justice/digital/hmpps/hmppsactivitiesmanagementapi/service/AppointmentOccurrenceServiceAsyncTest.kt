package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.AdditionalAnswers.returnsFirstArg
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDeletedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CancelAppointmentOccurrencesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UpdateAppointmentOccurrencesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import java.security.Principal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Optional

@ExtendWith(FakeSecurityContext::class)
class AppointmentOccurrenceServiceAsyncTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository = mock()
  private val auditService: AuditService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val appointmentOccurrenceUpdateDomainService = spy(AppointmentOccurrenceUpdateDomainService(appointmentRepository, telemetryClient, auditService))
  private val appointmentOccurrenceCancelDomainService = spy(AppointmentOccurrenceCancelDomainService(appointmentRepository, appointmentCancellationReasonRepository, telemetryClient, auditService))

  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val updateAppointmentOccurrencesJob: UpdateAppointmentOccurrencesJob = mock()
  private val cancelAppointmentOccurrencesJob: CancelAppointmentOccurrencesJob = mock()

  private var updated = argumentCaptor<LocalDateTime>()
  private var cancelled = argumentCaptor<LocalDateTime>()
  private var startTimeInMs = argumentCaptor<Long>()

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

  private val appointmentCancelledReason = appointmentCancelledReason()
  private val appointmentDeletedReason = appointmentDeletedReason()

  @BeforeEach
  fun setUp() {
    addCaseloadIdToRequestHeader("TPR")
    whenever(locationService.getLocationsForAppointmentsMap("TPR"))
      .thenReturn(mapOf(456L to appointmentLocation(456, "TPR")))
    whenever(principal.name).thenReturn("TEST.USER")
    whenever(appointmentCancellationReasonRepository.findById(appointmentCancelledReason.appointmentCancellationReasonId)).thenReturn(
      Optional.of(appointmentCancelledReason),
    )
    whenever(appointmentCancellationReasonRepository.findById(appointmentDeletedReason.appointmentCancellationReasonId)).thenReturn(
      Optional.of(appointmentDeletedReason),
    )
    whenever(appointmentRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg<Appointment>())
  }

  @Test
  fun `update internal location synchronously when update applies to one occurrence with fifteen allocations`() {
    val prisonerNumberToBookingIdMap = (1L..15L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointment = appointmentEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      repeatPeriod = AppointmentRepeatPeriod.DAILY,
      numberOfOccurrences = 2,
    )
    val appointmentOccurrence = appointment.occurrences().first()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )

    val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456, applyTo = ApplyTo.THIS_OCCURRENCE)

    service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

    // Update all apply to occurrences synchronously and track custom event
    verify(appointmentOccurrenceUpdateDomainService).updateAppointmentOccurrences(
      eq(appointment),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(setOf(appointmentOccurrence)),
      eq(request),
      eq(emptyMap()),
      updated.capture(),
      eq("TEST.USER"),
      eq(1),
      eq(15),
      startTimeInMs.capture(),
      eq(true),
      eq(true),
    )

    assertThat(updated.firstValue).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(startTimeInMs.firstValue).isCloseTo(System.currentTimeMillis(), within(60000L))

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_EDITED.value), any(), any())

    // Do not start asynchronous job as update is complete
    verifyNoInteractions(updateAppointmentOccurrencesJob)
  }

  @Test
  fun `update internal location synchronously when update applies to two occurrence with seven allocations affecting fourteen in total`() {
    val prisonerNumberToBookingIdMap = (1L..7L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointment = appointmentEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      repeatPeriod = AppointmentRepeatPeriod.DAILY,
      numberOfOccurrences = 2,
    )
    val appointmentOccurrence = appointment.occurrences().first()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )

    val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456, applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)

    service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

    // Update all apply to occurrences synchronously and track custom event
    verify(appointmentOccurrenceUpdateDomainService).updateAppointmentOccurrences(
      eq(appointment),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(appointment.scheduledOccurrences().toSet()),
      eq(request),
      eq(emptyMap()),
      updated.capture(),
      eq("TEST.USER"),
      eq(2),
      eq(14),
      startTimeInMs.capture(),
      eq(true),
      eq(true),
    )

    assertThat(updated.firstValue).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(startTimeInMs.firstValue).isCloseTo(System.currentTimeMillis(), within(60000L))

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_EDITED.value), any(), any())

    // Do not start asynchronous job as update is complete
    verifyNoInteractions(updateAppointmentOccurrencesJob)
  }

  @Test
  fun `update internal location asynchronously when update applies to five occurrence with three allocations affecting fifteen in total`() {
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointment = appointmentEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      repeatPeriod = AppointmentRepeatPeriod.DAILY,
      numberOfOccurrences = 5,
    )
    val appointmentOccurrence = appointment.occurrences().first()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )

    val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456, applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)

    service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

    // Update only the first occurrence synchronously and do not track custom event
    verify(appointmentOccurrenceUpdateDomainService).updateAppointmentOccurrences(
      eq(appointment),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(setOf(appointmentOccurrence)),
      eq(request),
      eq(emptyMap()),
      updated.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
      eq(false),
      eq(true),
    )

    assertThat(updated.firstValue).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(startTimeInMs.firstValue).isCloseTo(System.currentTimeMillis(), within(60000L))

    verifyNoInteractions(telemetryClient)

    // Start asynchronous job to apply update to all remaining occurrences
    verify(updateAppointmentOccurrencesJob).execute(
      eq(appointment.appointmentId),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(appointment.scheduledOccurrences().filterNot { it.appointmentOccurrenceId == appointmentOccurrence.appointmentOccurrenceId }.map { it.appointmentOccurrenceId }.toSet()),
      eq(request),
      eq(emptyMap()),
      updated.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
    )

    // Use the same updated value so that all occurrences have the same updated date time stamp
    updated.firstValue isEqualTo updated.secondValue
    // Use the same start time so that elapsed time metric is calculated correctly and consistently with the synchronous path
    startTimeInMs.firstValue isEqualTo startTimeInMs.secondValue
  }

  @Test
  fun `remove prisoners synchronously when removing fourteen allocations across two occurrences`() {
    val prisonerNumberToBookingIdMap = (1L..7L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointment = appointmentEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      repeatPeriod = AppointmentRepeatPeriod.DAILY,
      numberOfOccurrences = 2,
    )
    val appointmentOccurrence = appointment.occurrences().first()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )

    val request = AppointmentOccurrenceUpdateRequest(removePrisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)

    service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

    // Update all apply to occurrences synchronously and track custom event
    verify(appointmentOccurrenceUpdateDomainService).updateAppointmentOccurrences(
      eq(appointment),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(appointment.scheduledOccurrences().toSet()),
      eq(request),
      eq(emptyMap()),
      updated.capture(),
      eq("TEST.USER"),
      eq(2),
      eq(14),
      startTimeInMs.capture(),
      eq(true),
      eq(true),
    )

    assertThat(updated.firstValue).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(startTimeInMs.firstValue).isCloseTo(System.currentTimeMillis(), within(60000L))

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_EDITED.value), any(), any())

    // Do not start asynchronous job as update is complete
    verifyNoInteractions(updateAppointmentOccurrencesJob)
  }

  @Test
  fun `remove prisoners asynchronously when removing fifteen allocations across five occurrences`() {
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointment = appointmentEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      repeatPeriod = AppointmentRepeatPeriod.DAILY,
      numberOfOccurrences = 5,
    )
    val appointmentOccurrence = appointment.occurrences().first()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )

    val request = AppointmentOccurrenceUpdateRequest(removePrisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)

    service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

    // Update only the first occurrence synchronously and do not track custom event
    verify(appointmentOccurrenceUpdateDomainService).updateAppointmentOccurrences(
      eq(appointment),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(setOf(appointmentOccurrence)),
      eq(request),
      eq(emptyMap()),
      updated.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
      eq(false),
      eq(true),
    )

    assertThat(updated.firstValue).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(startTimeInMs.firstValue).isCloseTo(System.currentTimeMillis(), within(60000L))

    verifyNoInteractions(telemetryClient)

    // Start asynchronous job to apply update to all remaining occurrences
    verify(updateAppointmentOccurrencesJob).execute(
      eq(appointment.appointmentId),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(appointment.scheduledOccurrences().filterNot { it.appointmentOccurrenceId == appointmentOccurrence.appointmentOccurrenceId }.map { it.appointmentOccurrenceId }.toSet()),
      eq(request),
      eq(emptyMap()),
      updated.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
    )

    // Use the same updated value so that all occurrences have the same updated date time stamp
    updated.firstValue isEqualTo updated.secondValue
    // Use the same start time so that elapsed time metric is calculated correctly and consistently with the synchronous path
    startTimeInMs.firstValue isEqualTo startTimeInMs.secondValue
  }

  @Test
  fun `add prisoners synchronously when adding fourteen allocations across two occurrences`() {
    val existingPrisonerNumberToBookingIdMap = (1L..2L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val addedPrisonerNumberToBookingIdMap = (3L..9L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointment = appointmentEntity(
      prisonerNumberToBookingIdMap = existingPrisonerNumberToBookingIdMap,
      repeatPeriod = AppointmentRepeatPeriod.DAILY,
      numberOfOccurrences = 2,
    )
    val appointmentOccurrence = appointment.occurrences().first()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )

    val request = AppointmentOccurrenceUpdateRequest(addPrisonerNumbers = addedPrisonerNumberToBookingIdMap.keys.toList(), applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)

    val prisoners = addedPrisonerNumberToBookingIdMap.map {
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = it.key,
        bookingId = it.value,
        prisonId = appointment.prisonCode,
      )
    }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!)).thenReturn(Mono.just(prisoners))

    service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

    // Update all apply to occurrences synchronously and track custom event
    verify(appointmentOccurrenceUpdateDomainService).updateAppointmentOccurrences(
      eq(appointment),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(appointment.scheduledOccurrences().toSet()),
      eq(request),
      eq(prisoners.associateBy { it.prisonerNumber }),
      updated.capture(),
      eq("TEST.USER"),
      eq(2),
      eq(14),
      startTimeInMs.capture(),
      eq(true),
      eq(true),
    )

    assertThat(updated.firstValue).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(startTimeInMs.firstValue).isCloseTo(System.currentTimeMillis(), within(60000L))

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_EDITED.value), any(), any())

    // Do not start asynchronous job as update is complete
    verifyNoInteractions(updateAppointmentOccurrencesJob)
  }

  @Test
  fun `add prisoners asynchronously when adding fifteen allocations across five occurrences`() {
    val existingPrisonerNumberToBookingIdMap = (1L..2L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val addedPrisonerNumberToBookingIdMap = (3L..5L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointment = appointmentEntity(
      prisonerNumberToBookingIdMap = existingPrisonerNumberToBookingIdMap,
      repeatPeriod = AppointmentRepeatPeriod.DAILY,
      numberOfOccurrences = 5,
    )
    val appointmentOccurrence = appointment.occurrences().first()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )

    val request = AppointmentOccurrenceUpdateRequest(addPrisonerNumbers = addedPrisonerNumberToBookingIdMap.keys.toList(), applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)

    val prisoners = addedPrisonerNumberToBookingIdMap.map {
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = it.key,
        bookingId = it.value,
        prisonId = appointment.prisonCode,
      )
    }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!)).thenReturn(Mono.just(prisoners))

    service.updateAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

    // Update only the first occurrence synchronously and do not track custom event
    verify(appointmentOccurrenceUpdateDomainService).updateAppointmentOccurrences(
      eq(appointment),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(setOf(appointmentOccurrence)),
      eq(request),
      eq(prisoners.associateBy { it.prisonerNumber }),
      updated.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
      eq(false),
      eq(true),
    )

    assertThat(updated.firstValue).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(startTimeInMs.firstValue).isCloseTo(System.currentTimeMillis(), within(60000L))

    verifyNoInteractions(telemetryClient)

    // Start asynchronous job to apply update to all remaining occurrences
    verify(updateAppointmentOccurrencesJob).execute(
      eq(appointment.appointmentId),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(appointment.scheduledOccurrences().filterNot { it.appointmentOccurrenceId == appointmentOccurrence.appointmentOccurrenceId }.map { it.appointmentOccurrenceId }.toSet()),
      eq(request),
      eq(prisoners.associateBy { it.prisonerNumber }),
      updated.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
    )

    // Use the same updated value so that all occurrences have the same updated date time stamp
    updated.firstValue isEqualTo updated.secondValue
    // Use the same start time so that elapsed time metric is calculated correctly and consistently with the synchronous path
    startTimeInMs.firstValue isEqualTo startTimeInMs.secondValue
  }

  @Test
  fun `cancel synchronously when cancel applies to one occurrence with fifteen allocations`() {
    val prisonerNumberToBookingIdMap = (1L..15L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointment = appointmentEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      repeatPeriod = AppointmentRepeatPeriod.DAILY,
      numberOfOccurrences = 2,
    )
    val appointmentOccurrence = appointment.occurrences().first()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )

    val request = AppointmentOccurrenceCancelRequest(
      cancellationReasonId = appointmentCancelledReason.appointmentCancellationReasonId,
      applyTo = ApplyTo.THIS_OCCURRENCE,
    )

    service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

    // Cancel all apply to occurrences synchronously and track custom event
    verify(appointmentOccurrenceCancelDomainService).cancelAppointmentOccurrences(
      eq(appointment),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(setOf(appointmentOccurrence)),
      eq(request),
      cancelled.capture(),
      eq("TEST.USER"),
      eq(1),
      eq(15),
      startTimeInMs.capture(),
      eq(true),
      eq(true),
    )

    assertThat(cancelled.firstValue).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(startTimeInMs.firstValue).isCloseTo(System.currentTimeMillis(), within(60000L))

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_CANCELLED.value), any(), any())

    // Do not start asynchronous job as cancel is complete
    verifyNoInteractions(cancelAppointmentOccurrencesJob)
  }

  @Test
  fun `cancel synchronously when cancel applies to two occurrence with seven allocations affecting fourteen in total`() {
    val prisonerNumberToBookingIdMap = (1L..7L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointment = appointmentEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      repeatPeriod = AppointmentRepeatPeriod.DAILY,
      numberOfOccurrences = 2,
    )
    val appointmentOccurrence = appointment.occurrences().first()
    val scheduledOccurrences = appointment.scheduledOccurrences().toSet()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )

    val request = AppointmentOccurrenceCancelRequest(
      cancellationReasonId = appointmentCancelledReason.appointmentCancellationReasonId,
      applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES,
    )

    service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

    // Cancel all apply to occurrences synchronously and track custom event
    verify(appointmentOccurrenceCancelDomainService).cancelAppointmentOccurrences(
      eq(appointment),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(scheduledOccurrences),
      eq(request),
      cancelled.capture(),
      eq("TEST.USER"),
      eq(2),
      eq(14),
      startTimeInMs.capture(),
      eq(true),
      eq(true),
    )

    assertThat(cancelled.firstValue).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(startTimeInMs.firstValue).isCloseTo(System.currentTimeMillis(), within(60000L))

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_CANCELLED.value), any(), any())

    // Do not start asynchronous job as cancel is complete
    verifyNoInteractions(cancelAppointmentOccurrencesJob)
  }

  @Test
  fun `cancel asynchronously when cancel applies to five occurrence with three allocations affecting fifteen in total`() {
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointment = appointmentEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      repeatPeriod = AppointmentRepeatPeriod.DAILY,
      numberOfOccurrences = 5,
    )
    val appointmentOccurrence = appointment.occurrences().first()
    val scheduledOccurrences = appointment.scheduledOccurrences().toSet()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )

    val request = AppointmentOccurrenceCancelRequest(
      cancellationReasonId = appointmentCancelledReason.appointmentCancellationReasonId,
      applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES,
    )

    service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

    // Cancel only the first occurrence synchronously and do not track custom event
    verify(appointmentOccurrenceCancelDomainService).cancelAppointmentOccurrences(
      eq(appointment),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(setOf(appointmentOccurrence)),
      eq(request),
      cancelled.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
      eq(false),
      eq(true),
    )

    assertThat(cancelled.firstValue).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(startTimeInMs.firstValue).isCloseTo(System.currentTimeMillis(), within(60000L))

    verifyNoInteractions(telemetryClient)

    // Start asynchronous job to cancel all remaining occurrences
    verify(cancelAppointmentOccurrencesJob).execute(
      eq(appointment.appointmentId),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(scheduledOccurrences.filterNot { it.appointmentOccurrenceId == appointmentOccurrence.appointmentOccurrenceId }.map { it.appointmentOccurrenceId }.toSet()),
      eq(request),
      cancelled.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
    )

    // Use the same cancelled value so that all occurrences have the same cancelled date time stamp
    cancelled.firstValue isEqualTo cancelled.secondValue
    // Use the same start time so that elapsed time metric is calculated correctly and consistently with the synchronous path
    startTimeInMs.firstValue isEqualTo startTimeInMs.secondValue
  }

  @Test
  fun `delete synchronously when delete applies to one occurrence with fifteen allocations`() {
    val prisonerNumberToBookingIdMap = (1L..15L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointment = appointmentEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      repeatPeriod = AppointmentRepeatPeriod.DAILY,
      numberOfOccurrences = 2,
    )
    val appointmentOccurrence = appointment.occurrences().first()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )

    val request = AppointmentOccurrenceCancelRequest(
      cancellationReasonId = appointmentDeletedReason.appointmentCancellationReasonId,
      applyTo = ApplyTo.THIS_OCCURRENCE,
    )

    service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

    // Delete all apply to occurrences synchronously and track custom event
    verify(appointmentOccurrenceCancelDomainService).cancelAppointmentOccurrences(
      eq(appointment),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(setOf(appointmentOccurrence)),
      eq(request),
      cancelled.capture(),
      eq("TEST.USER"),
      eq(1),
      eq(15),
      startTimeInMs.capture(),
      eq(true),
      eq(true),
    )

    assertThat(cancelled.firstValue).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(startTimeInMs.firstValue).isCloseTo(System.currentTimeMillis(), within(60000L))

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_DELETED.value), any(), any())

    // Do not start asynchronous job as delete is complete
    verifyNoInteractions(cancelAppointmentOccurrencesJob)
  }

  @Test
  fun `delete synchronously when delete applies to two occurrence with seven allocations affecting fourteen in total`() {
    val prisonerNumberToBookingIdMap = (1L..7L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointment = appointmentEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      repeatPeriod = AppointmentRepeatPeriod.DAILY,
      numberOfOccurrences = 2,
    )
    val appointmentOccurrence = appointment.occurrences().first()
    val scheduledOccurrences = appointment.scheduledOccurrences().toSet()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )

    val request = AppointmentOccurrenceCancelRequest(
      cancellationReasonId = appointmentDeletedReason.appointmentCancellationReasonId,
      applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES,
    )

    service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

    // Delete all apply to occurrences synchronously and track custom event
    verify(appointmentOccurrenceCancelDomainService).cancelAppointmentOccurrences(
      eq(appointment),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(scheduledOccurrences),
      eq(request),
      cancelled.capture(),
      eq("TEST.USER"),
      eq(2),
      eq(14),
      startTimeInMs.capture(),
      eq(true),
      eq(true),
    )

    assertThat(cancelled.firstValue).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(startTimeInMs.firstValue).isCloseTo(System.currentTimeMillis(), within(60000L))

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_DELETED.value), any(), any())

    // Do not start asynchronous job as delete is complete
    verifyNoInteractions(cancelAppointmentOccurrencesJob)
  }

  @Test
  fun `delete asynchronously when delete applies to five occurrence with three allocations affecting fifteen in total`() {
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointment = appointmentEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      repeatPeriod = AppointmentRepeatPeriod.DAILY,
      numberOfOccurrences = 5,
    )
    val appointmentOccurrence = appointment.occurrences().first()
    val scheduledOccurrences = appointment.scheduledOccurrences().toSet()
    whenever(appointmentOccurrenceRepository.findById(appointmentOccurrence.appointmentOccurrenceId)).thenReturn(
      Optional.of(appointmentOccurrence),
    )

    val request = AppointmentOccurrenceCancelRequest(
      cancellationReasonId = appointmentDeletedReason.appointmentCancellationReasonId,
      applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES,
    )

    service.cancelAppointmentOccurrence(appointmentOccurrence.appointmentOccurrenceId, request, principal)

    // Delete only the first occurrence synchronously and do not track custom event
    verify(appointmentOccurrenceCancelDomainService).cancelAppointmentOccurrences(
      eq(appointment),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(setOf(appointmentOccurrence)),
      eq(request),
      cancelled.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
      eq(false),
      eq(true),
    )

    assertThat(cancelled.firstValue).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(startTimeInMs.firstValue).isCloseTo(System.currentTimeMillis(), within(60000L))

    verifyNoInteractions(telemetryClient)

    // Start asynchronous job to delete all remaining occurrences
    verify(cancelAppointmentOccurrencesJob).execute(
      eq(appointment.appointmentId),
      eq(appointmentOccurrence.appointmentOccurrenceId),
      eq(scheduledOccurrences.filterNot { it.appointmentOccurrenceId == appointmentOccurrence.appointmentOccurrenceId }.map { it.appointmentOccurrenceId }.toSet()),
      eq(request),
      cancelled.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
    )

    // Use the same cancelled value so that all occurrences have the same cancelled date time stamp
    cancelled.firstValue isEqualTo cancelled.secondValue
    // Use the same start time so that elapsed time metric is calculated correctly and consistently with the synchronous path
    startTimeInMs.firstValue isEqualTo startTimeInMs.secondValue
  }
}
