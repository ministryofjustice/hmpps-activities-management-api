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
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCreatedInErrorReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.permanentRemovalByUserAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CancelAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UncancelAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UpdateAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventOrganiserRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import java.security.Principal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@ExtendWith(FakeSecurityContext::class)
class AppointmentServiceAsyncTest {
  private val appointmentSeriesRepository: AppointmentSeriesRepository = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentAttendeeRemovalReasonRepository: AppointmentAttendeeRemovalReasonRepository = mock()
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val auditService: AuditService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val eventTierRepository: EventTierRepository = mock()
  private val eventOrganiserRepository: EventOrganiserRepository = mock()
  private val locationService: LocationService = mock()

  private val appointmentUpdateDomainService = spy(
    AppointmentUpdateDomainService(
      appointmentSeriesRepository,
      appointmentAttendeeRemovalReasonRepository,
      eventTierRepository,
      eventOrganiserRepository,
      TransactionHandler(),
      outboundEventsService,
      telemetryClient,
      auditService,
      locationService,
    ),
  )
  private val appointmentCancelDomainService = spy(
    AppointmentCancelDomainService(
      appointmentSeriesRepository,
      appointmentCancellationReasonRepository,
      telemetryClient,
      auditService,
      TransactionHandler(),
      outboundEventsService,
    ),
  )

  private val referenceCodeService: ReferenceCodeService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val updateAppointmentsJob: UpdateAppointmentsJob = mock()
  private val cancelAppointmentsJob: CancelAppointmentsJob = mock()
  private val uncancelAppointmentsJob: UncancelAppointmentsJob = mock()

  private var updated = argumentCaptor<LocalDateTime>()
  private var cancelled = argumentCaptor<LocalDateTime>()
  private var startTimeInMs = argumentCaptor<Long>()

  private val service = AppointmentService(
    appointmentRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    appointmentUpdateDomainService,
    appointmentCancelDomainService,
    updateAppointmentsJob,
    cancelAppointmentsJob,
    uncancelAppointmentsJob,
    maxSyncAppointmentInstanceActions = 14,
  )

  private val principal: Principal = mock()

  private val permanentRemovalByUserAppointmentAttendeeRemovalReason = permanentRemovalByUserAppointmentAttendeeRemovalReason()
  private val appointmentCancelledReason = appointmentCancelledReason()
  private val appointmentDeletedReason = appointmentCreatedInErrorReason()

  private val dpsLocationId = UUID.fromString("44444444-1111-2222-3333-444444444444")

  @BeforeEach
  fun setUp() {
    addCaseloadIdToRequestHeader("TPR")

    whenever(locationService.getLocationDetailsForAppointmentsMap("TPR"))
      .thenReturn(mapOf(456L to appointmentLocationDetails(456, dpsLocationId, "TPR")))

    whenever(locationService.getLocationDetailsForAppointmentsMapByDpsLocationId("TPR"))
      .thenReturn(mapOf(dpsLocationId to appointmentLocationDetails(456, dpsLocationId, "TPR")))

    whenever(principal.name).thenReturn("TEST.USER")
    whenever(appointmentAttendeeRemovalReasonRepository.findById(permanentRemovalByUserAppointmentAttendeeRemovalReason.appointmentAttendeeRemovalReasonId)).thenReturn(
      Optional.of(permanentRemovalByUserAppointmentAttendeeRemovalReason),
    )
    whenever(appointmentCancellationReasonRepository.findById(appointmentCancelledReason.appointmentCancellationReasonId)).thenReturn(
      Optional.of(appointmentCancelledReason),
    )
    whenever(appointmentCancellationReasonRepository.findById(appointmentDeletedReason.appointmentCancellationReasonId)).thenReturn(
      Optional.of(appointmentDeletedReason),
    )
    whenever(appointmentSeriesRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg<AppointmentSeries>())
  }

  @Test
  fun `update DPS location synchronously when update applies to one appointment with fifteen attendees`() {
    val prisonerNumberToBookingIdMap = (1L..15L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 2,
    )
    val appointment = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )
    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId))
      .thenReturn(Optional.of(appointmentSeries))

    val request = AppointmentUpdateRequest(dpsLocationId = dpsLocationId, applyTo = ApplyTo.THIS_APPOINTMENT)

    service.updateAppointment(appointment.appointmentId, request, principal)

    // Update all apply to appointments synchronously and track custom event
    verify(appointmentUpdateDomainService).updateAppointments(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(setOf(appointment.appointmentId)),
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
    verifyNoInteractions(updateAppointmentsJob)
  }

  @Test
  fun `update internal location synchronously when update applies to two appointments with seven attendees affecting fourteen in total`() {
    val prisonerNumberToBookingIdMap = (1L..7L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 2,
    )
    val appointment = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )
    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId))
      .thenReturn(Optional.of(appointmentSeries))

    val request = AppointmentUpdateRequest(internalLocationId = 456, dpsLocationId = dpsLocationId, applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS)

    service.updateAppointment(appointment.appointmentId, request, principal)

    // Update all apply to appointments synchronously and track custom event
    verify(appointmentUpdateDomainService).updateAppointments(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(appointmentSeries.scheduledAppointments().map { it.appointmentId }.toSet()),
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
    verifyNoInteractions(updateAppointmentsJob)
  }

  @Test
  fun `update internal location, using Nomis location id asynchronously when update applies to five appointments with three attendees affecting fifteen in total`() {
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 5,
    )
    val appointment = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )
    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId))
      .thenReturn(Optional.of(appointmentSeries))

    val request = AppointmentUpdateRequest(internalLocationId = 456, dpsLocationId = dpsLocationId, applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS)

    service.updateAppointment(appointment.appointmentId, request, principal)

    // Update only the first appointment synchronously and do not track custom event
    verify(appointmentUpdateDomainService).updateAppointments(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(setOf(appointment.appointmentId)),
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

    // Start asynchronous job to apply update to all remaining appointments
    verify(updateAppointmentsJob).execute(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(appointmentSeries.scheduledAppointments().filterNot { it.appointmentId == appointment.appointmentId }.map { it.appointmentId }.toSet()),
      eq(request),
      eq(emptyMap()),
      updated.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
    )

    // Use the same updated value so that all appointments have the same updated date time stamp
    updated.firstValue isEqualTo updated.secondValue
    // Use the same start time so that elapsed time metric is calculated correctly and consistently with the synchronous path
    startTimeInMs.firstValue isEqualTo startTimeInMs.secondValue
  }

  @Test
  fun `update internal location, using DPS location id, asynchronously when update applies to five appointments with three attendees affecting fifteen in total`() {
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 5,
    )
    val appointment = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )
    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId))
      .thenReturn(Optional.of(appointmentSeries))

    val request = AppointmentUpdateRequest(dpsLocationId = dpsLocationId, applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS)

    service.updateAppointment(appointment.appointmentId, request, principal)

    // Update only the first appointment synchronously and do not track custom event
    verify(appointmentUpdateDomainService).updateAppointments(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(setOf(appointment.appointmentId)),
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

    // Start asynchronous job to apply update to all remaining appointments
    verify(updateAppointmentsJob).execute(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(appointmentSeries.scheduledAppointments().filterNot { it.appointmentId == appointment.appointmentId }.map { it.appointmentId }.toSet()),
      eq(request),
      eq(emptyMap()),
      updated.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
    )

    // Use the same updated value so that all appointments have the same updated date time stamp
    updated.firstValue isEqualTo updated.secondValue
    // Use the same start time so that elapsed time metric is calculated correctly and consistently with the synchronous path
    startTimeInMs.firstValue isEqualTo startTimeInMs.secondValue
  }

  @Test
  fun `remove prisoners synchronously when removing fourteen attendees across two appointments`() {
    val prisonerNumberToBookingIdMap = (1L..7L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 2,
    )
    val appointment = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )
    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId))
      .thenReturn(Optional.of(appointmentSeries))

    val request = AppointmentUpdateRequest(removePrisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS)

    service.updateAppointment(appointment.appointmentId, request, principal)

    // Update all apply to appointments synchronously and track custom event
    verify(appointmentUpdateDomainService).updateAppointments(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(appointmentSeries.scheduledAppointments().map { it.appointmentId }.toSet()),
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
    verifyNoInteractions(updateAppointmentsJob)
  }

  @Test
  fun `remove prisoners asynchronously when removing fifteen attendees across five appointments`() {
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 5,
    )
    val appointment = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )
    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId))
      .thenReturn(Optional.of(appointmentSeries))

    val request = AppointmentUpdateRequest(removePrisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS)

    service.updateAppointment(appointment.appointmentId, request, principal)

    // Update only the first appointment synchronously and do not track custom event
    verify(appointmentUpdateDomainService).updateAppointments(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(setOf(appointment.appointmentId)),
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

    // Start asynchronous job to apply update to all remaining appointments
    verify(updateAppointmentsJob).execute(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(appointmentSeries.scheduledAppointments().filterNot { it.appointmentId == appointment.appointmentId }.map { it.appointmentId }.toSet()),
      eq(request),
      eq(emptyMap()),
      updated.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
    )

    // Use the same updated value so that all appointments have the same updated date time stamp
    updated.firstValue isEqualTo updated.secondValue
    // Use the same start time so that elapsed time metric is calculated correctly and consistently with the synchronous path
    startTimeInMs.firstValue isEqualTo startTimeInMs.secondValue
  }

  @Test
  fun `add prisoners synchronously when adding fourteen attendees across two appointments`() {
    val existingPrisonerNumberToBookingIdMap = (1L..2L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val addedPrisonerNumberToBookingIdMap = (3L..9L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = existingPrisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 2,
    )
    val appointment = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )
    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId))
      .thenReturn(Optional.of(appointmentSeries))

    val request = AppointmentUpdateRequest(addPrisonerNumbers = addedPrisonerNumberToBookingIdMap.keys.toList(), applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS)

    val prisoners = addedPrisonerNumberToBookingIdMap.map {
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = it.key,
        bookingId = it.value,
        prisonId = appointmentSeries.prisonCode,
      )
    }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!)).thenReturn(prisoners)

    service.updateAppointment(appointment.appointmentId, request, principal)

    // Update all apply to appointments synchronously and track custom event
    verify(appointmentUpdateDomainService).updateAppointments(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(appointmentSeries.scheduledAppointments().map { it.appointmentId }.toSet()),
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
    verifyNoInteractions(updateAppointmentsJob)
  }

  @Test
  fun `add prisoners asynchronously when adding fifteen attendees across five appointments`() {
    val existingPrisonerNumberToBookingIdMap = (1L..2L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val addedPrisonerNumberToBookingIdMap = (3L..5L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = existingPrisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 5,
    )
    val appointment = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )
    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId))
      .thenReturn(Optional.of(appointmentSeries))

    val request = AppointmentUpdateRequest(addPrisonerNumbers = addedPrisonerNumberToBookingIdMap.keys.toList(), applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS)

    val prisoners = addedPrisonerNumberToBookingIdMap.map {
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = it.key,
        bookingId = it.value,
        prisonId = appointmentSeries.prisonCode,
      )
    }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers!!)).thenReturn(prisoners)

    service.updateAppointment(appointment.appointmentId, request, principal)

    // Update only the first appointment synchronously and do not track custom event
    verify(appointmentUpdateDomainService).updateAppointments(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(setOf(appointment.appointmentId)),
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

    // Start asynchronous job to apply update to all remaining appointments
    verify(updateAppointmentsJob).execute(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(appointmentSeries.scheduledAppointments().filterNot { it.appointmentId == appointment.appointmentId }.map { it.appointmentId }.toSet()),
      eq(request),
      eq(prisoners.associateBy { it.prisonerNumber }),
      updated.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
    )

    // Use the same updated value so that all appointments have the same updated date time stamp
    updated.firstValue isEqualTo updated.secondValue
    // Use the same start time so that elapsed time metric is calculated correctly and consistently with the synchronous path
    startTimeInMs.firstValue isEqualTo startTimeInMs.secondValue
  }

  @Test
  fun `cancel synchronously when cancel applies to one appointment with fifteen attendees`() {
    val prisonerNumberToBookingIdMap = (1L..15L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 2,
    )
    val appointment = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )

    val request = AppointmentCancelRequest(
      cancellationReasonId = appointmentCancelledReason.appointmentCancellationReasonId,
      applyTo = ApplyTo.THIS_APPOINTMENT,
    )

    service.cancelAppointment(appointment.appointmentId, request, principal)

    // Cancel all apply to appointments synchronously and track custom event
    verify(appointmentCancelDomainService).cancelAppointments(
      eq(appointmentSeries),
      eq(appointment.appointmentId),
      eq(setOf(appointment)),
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

    appointment.attendees().forEach {
      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED, it.appointmentAttendeeId)
    }
    verifyNoMoreInteractions(outboundEventsService)

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_CANCELLED.value), any(), any())
    verify(auditService).logEvent(any<AppointmentCancelledEvent>())

    // Do not start asynchronous job as cancel is complete
    verifyNoInteractions(cancelAppointmentsJob)
  }

  @Test
  fun `cancel synchronously when cancel applies to two appointments with seven attendees affecting fourteen in total`() {
    val prisonerNumberToBookingIdMap = (1L..7L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 2,
    )
    val appointment = appointmentSeries.appointments().first()
    val scheduledAppointments = appointmentSeries.scheduledAppointments().toSet()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )

    val request = AppointmentCancelRequest(
      cancellationReasonId = appointmentCancelledReason.appointmentCancellationReasonId,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    service.cancelAppointment(appointment.appointmentId, request, principal)

    // these will be null when the cancelAppointments(...) function is called
    appointmentSeries.cancelledBy = null
    appointmentSeries.cancelledTime = null
    appointmentSeries.cancellationStartTime = null
    appointmentSeries.cancellationStartDate = null

    // Cancel all apply to appointments synchronously and track custom event
    verify(appointmentCancelDomainService).cancelAppointments(
      eq(appointmentSeries),
      eq(appointment.appointmentId),
      eq(scheduledAppointments),
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

    scheduledAppointments.forEach {
      it.attendees().forEach { attendee ->
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED, attendee.appointmentAttendeeId)
      }
    }
    verifyNoMoreInteractions(outboundEventsService)

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_CANCELLED.value), any(), any())
    verify(auditService).logEvent(any<AppointmentCancelledEvent>())

    // Do not start asynchronous job as cancel is complete
    verifyNoInteractions(cancelAppointmentsJob)
  }

  @Test
  fun `cancel asynchronously when cancel applies to five appointments with three attendees affecting fifteen in total`() {
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 5,
    )
    val appointment = appointmentSeries.appointments().first()
    val scheduledAppointments = appointmentSeries.scheduledAppointments().toSet()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )

    val request = AppointmentCancelRequest(
      cancellationReasonId = appointmentCancelledReason.appointmentCancellationReasonId,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    service.cancelAppointment(appointment.appointmentId, request, principal)

    // Cancel only the first appointment synchronously and do not track custom event
    verify(appointmentCancelDomainService).cancelAppointments(
      eq(appointmentSeries),
      eq(appointment.appointmentId),
      eq(setOf(appointment)),
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

    appointment.attendees().forEach { attendee ->
      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED, attendee.appointmentAttendeeId)
    }
    verifyNoMoreInteractions(outboundEventsService)

    verifyNoInteractions(telemetryClient)

    // Start asynchronous job to cancel all remaining appointments
    verify(cancelAppointmentsJob).execute(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(scheduledAppointments.filterNot { it.appointmentId == appointment.appointmentId }.map { it.appointmentId }.toSet()),
      eq(request),
      cancelled.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
    )

    verify(auditService).logEvent(any<AppointmentCancelledEvent>())

    // Use the same cancelled value so that all appointments have the same cancelled date time stamp
    cancelled.firstValue isEqualTo cancelled.secondValue
    // Use the same start time so that elapsed time metric is calculated correctly and consistently with the synchronous path
    startTimeInMs.firstValue isEqualTo startTimeInMs.secondValue
  }

  @Test
  fun `delete synchronously when delete applies to one appointment with fifteen attendees`() {
    val prisonerNumberToBookingIdMap = (1L..15L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 2,
    )
    val appointment = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )

    val request = AppointmentCancelRequest(
      cancellationReasonId = appointmentDeletedReason.appointmentCancellationReasonId,
      applyTo = ApplyTo.THIS_APPOINTMENT,
    )

    service.cancelAppointment(appointment.appointmentId, request, principal)

    // Delete all apply to appointments synchronously and track custom event
    verify(appointmentCancelDomainService).cancelAppointments(
      eq(appointmentSeries),
      eq(appointment.appointmentId),
      eq(setOf(appointment)),
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

    appointment.attendees().forEach { attendee ->
      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, attendee.appointmentAttendeeId)
    }
    verifyNoMoreInteractions(outboundEventsService)

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_DELETED.value), any(), any())
    verify(auditService).logEvent(any<AppointmentDeletedEvent>())

    // Do not start asynchronous job as delete is complete
    verifyNoInteractions(cancelAppointmentsJob)
  }

  @Test
  fun `delete synchronously when delete applies to two appointments with seven attendees affecting fourteen in total`() {
    val prisonerNumberToBookingIdMap = (1L..7L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 2,
    )
    val appointment = appointmentSeries.appointments().first()
    val scheduledAppointments = appointmentSeries.scheduledAppointments().toSet()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )

    val request = AppointmentCancelRequest(
      cancellationReasonId = appointmentDeletedReason.appointmentCancellationReasonId,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    service.cancelAppointment(appointment.appointmentId, request, principal)

    // these will be null when the cancelAppointments(...) function is called
    appointmentSeries.cancelledBy = null
    appointmentSeries.cancelledTime = null
    appointmentSeries.cancellationStartTime = null
    appointmentSeries.cancellationStartDate = null

    // Delete all apply to appointments synchronously and track custom event
    verify(appointmentCancelDomainService).cancelAppointments(
      eq(appointmentSeries),
      eq(appointment.appointmentId),
      eq(scheduledAppointments),
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

    scheduledAppointments.forEach {
      it.attendees().forEach { attendee ->
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, attendee.appointmentAttendeeId)
      }
    }
    verifyNoMoreInteractions(outboundEventsService)

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_DELETED.value), any(), any())
    verify(auditService).logEvent(any<AppointmentDeletedEvent>())

    // Do not start asynchronous job as delete is complete
    verifyNoInteractions(cancelAppointmentsJob)
  }

  @Test
  fun `delete asynchronously when delete applies to five appointments with three attendees affecting fifteen in total`() {
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap,
      frequency = AppointmentFrequency.DAILY,
      numberOfAppointments = 5,
    )
    val appointment = appointmentSeries.appointments().first()
    val scheduledAppointments = appointmentSeries.scheduledAppointments().toSet()
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(
      Optional.of(appointment),
    )

    val request = AppointmentCancelRequest(
      cancellationReasonId = appointmentDeletedReason.appointmentCancellationReasonId,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    service.cancelAppointment(appointment.appointmentId, request, principal)

    // Delete only the first appointment synchronously and do not track custom event
    verify(appointmentCancelDomainService).cancelAppointments(
      eq(appointmentSeries),
      eq(appointment.appointmentId),
      eq(setOf(appointment)),
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

    appointment.attendees().forEach { attendee ->
      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, attendee.appointmentAttendeeId)
    }
    verifyNoMoreInteractions(outboundEventsService)

    verifyNoInteractions(telemetryClient)
    verify(auditService).logEvent(any<AppointmentDeletedEvent>())

    // Start asynchronous job to delete all remaining appointments
    verify(cancelAppointmentsJob).execute(
      eq(appointmentSeries.appointmentSeriesId),
      eq(appointment.appointmentId),
      eq(scheduledAppointments.filterNot { it.appointmentId == appointment.appointmentId }.map { it.appointmentId }.toSet()),
      eq(request),
      cancelled.capture(),
      eq("TEST.USER"),
      eq(5),
      eq(15),
      startTimeInMs.capture(),
    )

    // Use the same cancelled value so that all appointments have the same cancelled date time stamp
    cancelled.firstValue isEqualTo cancelled.secondValue
    // Use the same start time so that elapsed time metric is calculated correctly and consistently with the synchronous path
    startTimeInMs.firstValue isEqualTo startTimeInMs.secondValue
  }
}
