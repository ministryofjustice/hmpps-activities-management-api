package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.foundationTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.locationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.permanentRemovalByUserAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentEditedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventOrganiserRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventTier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

@ExtendWith(FakeSecurityContext::class)
class AppointmentUpdateDomainServiceTest {
  private val appointmentSeriesRepository: AppointmentSeriesRepository = mock()
  private val appointmentAttendeeRemovalReasonRepository: AppointmentAttendeeRemovalReasonRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val eventTierRepository: EventTierRepository = mock()
  private val eventOrganiserRepository: EventOrganiserRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val auditService: AuditService = mock()
  private val locationService: LocationService = mock()

  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()
  private val service = spy(
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

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointmentSeries = appointmentSeriesEntity(updatedBy = null, prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 4)
  private val appointment = appointmentSeries.appointments()[1]
  private val applyToThis = appointmentSeries.applyToAppointments(appointment, ApplyTo.THIS_APPOINTMENT, "", false).toSet()
  private val applyToThisAndAllFuture = appointmentSeries.applyToAppointments(appointment, ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS, "", false).toSet()
  private val applyToAllFuture = appointmentSeries.applyToAppointments(appointment, ApplyTo.ALL_FUTURE_APPOINTMENTS, "", false).toSet()
  private val updatedBy = "TEST.USER"
  private val permanentRemovalByUserAppointmentAttendeeRemovalReason = permanentRemovalByUserAppointmentAttendeeRemovalReason()

  @BeforeEach
  fun setUp() {
    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeries))
    whenever(appointmentSeriesRepository.saveAndFlush(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<AppointmentSeries>())
    whenever(appointmentAttendeeRemovalReasonRepository.findById(permanentRemovalByUserAppointmentAttendeeRemovalReason.appointmentAttendeeRemovalReasonId)).thenReturn(
      Optional.of(permanentRemovalByUserAppointmentAttendeeRemovalReason),
    )
  }

  @Nested
  @DisplayName("update appointments - used by service and async update appointments job")
  inner class UpdateAppointments {
    @Test
    fun `updates category code`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentUpdateRequest(categoryCode = "NEW")
      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.categoryCode isEqualTo "TEST"
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.all { it.categoryCode == "NEW" } isBool true
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.all { it.categoryCode == "TEST" } isBool true

      response.categoryCode isEqualTo "TEST"
      response.appointments.filter { ids.contains(it.id) }.all { it.categoryCode == "NEW" } isBool true
      response.appointments.filterNot { ids.contains(it.id) }.all { it.categoryCode == "TEST" } isBool true

      verify(outboundEventsService, times(9)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED), any(), eq(null))
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `updates appointment tier`() {
      whenever(eventTierRepository.findByCode(foundationTier().code)).thenReturn(foundationTier())
      val appointmentsToUpdate = applyToThisAndAllFuture.onEach {
        it.appointmentTier = eventTier(1, "TIER_1", "Tier 1")
      }
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentUpdateRequest(tierCode = "FOUNDATION")
      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      val currentEventTier = EventTier(
        eventTierId = 2,
        code = "TIER_2",
        description = "Tier 2",
      )
      val newEventTier = EventTier(
        eventTierId = 3,
        code = "FOUNDATION",
        description = "Routine activities also called \"Foundation\"",
      )

      appointmentSeries.appointmentTier isEqualTo currentEventTier
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.all { it.appointmentTier == newEventTier } isBool true
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.all { it.appointmentTier == currentEventTier } isBool true

      response.tier isEqualTo currentEventTier.toModelEventTier()
      response.appointments.filter { ids.contains(it.id) }.all { it.tier == newEventTier.toModelEventTier() } isBool true
      response.appointments.filterNot { ids.contains(it.id) }.all { it.tier == currentEventTier.toModelEventTier() } isBool true
    }

    @Test
    fun `updates appointment organiser`() {
      val currentOrganiser = EventOrganiser(
        eventOrganiserId = 1,
        code = "PRISON_STAFF",
        description = "Prison staff",
      )
      val newOrganiser = EventOrganiser(
        eventOrganiserId = 2,
        code = "PRISONER",
        description = "A prisoner or group of prisoners",
      )

      whenever(eventTierRepository.findByCode(eventTier().code)).thenReturn(eventTier())
      whenever(eventOrganiserRepository.findByCode(newOrganiser.code)).thenReturn(newOrganiser)

      val appointmentsToUpdate = applyToThisAndAllFuture.onEach {
        it.appointmentTier = eventTier(2, "TIER_2", "Tier 2")
        it.appointmentOrganiser = currentOrganiser
      }
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentUpdateRequest(organiserCode = "PRISONER")
      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.appointmentOrganiser isEqualTo currentOrganiser
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.all { it.appointmentOrganiser == newOrganiser } isBool true
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.all { it.appointmentOrganiser == currentOrganiser } isBool true

      response.organiser isEqualTo currentOrganiser.toModelEventOrganiser()
      response.appointments.filter { ids.contains(it.id) }.all { it.organiser == newOrganiser.toModelEventOrganiser() } isBool true
      response.appointments.filterNot { ids.contains(it.id) }.all { it.organiser == currentOrganiser.toModelEventOrganiser() } isBool true
    }

    @Test
    fun `Removes appointment organiser when updating to tier other than TIER_2`() {
      whenever(eventTierRepository.findByCode(foundationTier().code)).thenReturn(foundationTier())

      val currentOrganiser = EventOrganiser(
        eventOrganiserId = 1,
        code = "PRISON_STAFF",
        description = "Prison staff",
      )

      val appointmentsToUpdate = applyToThisAndAllFuture.onEach {
        it.appointmentTier = eventTier(2, "TIER_2", "Tier 2")
        it.appointmentOrganiser = currentOrganiser
      }
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentUpdateRequest(tierCode = "FOUNDATION")
      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.appointmentOrganiser isEqualTo currentOrganiser
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.all { it.appointmentOrganiser == null } isBool true
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.all { it.appointmentOrganiser == currentOrganiser } isBool true

      response.organiser isEqualTo currentOrganiser.toModelEventOrganiser()
      response.appointments.filter { ids.contains(it.id) }.all { it.organiser == null } isBool true
      response.appointments.filterNot { ids.contains(it.id) }.all { it.organiser == currentOrganiser.toModelEventOrganiser() } isBool true
    }

    @Test
    fun `updates internal location id`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val dpsLocationId = UUID.fromString("44444444-1111-2222-3333-444444444444")
      val request = AppointmentUpdateRequest(internalLocationId = 456)

      whenever(locationService.getLocationDetails(456, null)).thenReturn(locationDetails(456, dpsLocationId, "TPR"))

      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      /* then the series will not be updated */
      appointmentSeries.internalLocationId isEqualTo 123
      /* then the selected appointments will be updated */
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.all { it.internalLocationId == 456L && it.dpsLocationId == dpsLocationId } isBool true
      /* the remaining appointments will not be updated */
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.all { it.internalLocationId == 123L } isBool true

      response.internalLocationId isEqualTo 123
      response.appointments.filter { ids.contains(it.id) }.all { it.internalLocationId == 456L } isBool true
      response.appointments.filterNot { ids.contains(it.id) }.all { it.internalLocationId == 123L } isBool true

      verify(outboundEventsService, times(9)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED), any(), eq(null))
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `updates DPS location id`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val dpsLocationId = UUID.fromString("44444444-1111-2222-3333-444444444444")
      val request = AppointmentUpdateRequest(dpsLocationId = dpsLocationId)

      whenever(locationService.getLocationDetails(null, dpsLocationId)).thenReturn(locationDetails(456, dpsLocationId, "TPR"))

      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      /* then the series will not be updated */
      appointmentSeries.internalLocationId isEqualTo 123
      /* then the selected appointments will be updated */
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.all { it.internalLocationId == 456L && it.dpsLocationId == dpsLocationId } isBool true
      /* the remaining appointments will not be updated */
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.all { it.internalLocationId == 123L } isBool true

      response.internalLocationId isEqualTo 123
      response.appointments.filter { ids.contains(it.id) }.all { it.internalLocationId == 456L } isBool true
      response.appointments.filterNot { ids.contains(it.id) }.all { it.internalLocationId == 123L } isBool true

      verify(outboundEventsService, times(9)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED), any(), eq(null))
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `updates in cell = true`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentUpdateRequest(inCell = true)
      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      /* then the series will not be updated */
      appointmentSeries.internalLocationId isEqualTo 123
      appointmentSeries.inCell isEqualTo false
      appointmentSeries.onWing isEqualTo false
      appointmentSeries.offWing isEqualTo true
      /* then the selected appointments will be updated */
      with(appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }) {
        this.all { it.internalLocationId == null } isBool true
        this.all { it.inCell } isBool true
        this.all { it.onWing } isBool true
        this.all { it.offWing } isBool false
      }
      /* then the other appointments will not be updated */
      with(appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }) {
        this.all { it.internalLocationId == 123L } isBool true
        this.all { it.inCell } isBool false
        this.all { it.onWing } isBool false
        this.all { it.offWing } isBool true
      }

      response.internalLocationId isEqualTo 123
      response.inCell isEqualTo false
      with(response.appointments.filter { ids.contains(it.id) }) {
        this.all { it.internalLocationId == null } isBool true
        this.all { it.inCell } isBool true
      }
      with(response.appointments.filterNot { ids.contains(it.id) }) {
        this.all { it.internalLocationId == 123L } isBool true
        this.all { !it.inCell } isBool true
      }

      verify(outboundEventsService, times(9)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED), any(), eq(null))
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `updates start date`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val weekFromNow = LocalDate.now().plusWeeks(2)
      val request = AppointmentUpdateRequest(startDate = weekFromNow)
      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        appointmentsToUpdate.map { it.appointmentId }.toSet(),
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.startDate isEqualTo LocalDate.now().plusDays(1)
      with(appointmentSeries.appointments()) {
        get(0).startDate isEqualTo LocalDate.now().plusDays(1)
        get(1).startDate isEqualTo weekFromNow
        get(2).startDate isEqualTo weekFromNow.plusDays(1)
        get(3).startDate isEqualTo weekFromNow.plusDays(2)
      }

      response.startDate isEqualTo LocalDate.now().plusDays(1)
      with(response.appointments) {
        get(0).startDate isEqualTo LocalDate.now().plusDays(1)
        get(1).startDate isEqualTo weekFromNow
        get(2).startDate isEqualTo weekFromNow.plusDays(1)
        get(3).startDate isEqualTo weekFromNow.plusDays(2)
      }

      verify(outboundEventsService, times(9)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED), any(), eq(null))
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `updates start time`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentUpdateRequest(startTime = LocalTime.of(13, 30))
      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.startTime isEqualTo LocalTime.of(9, 0)
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.all { it.startTime == LocalTime.of(13, 30) } isBool true
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.all { it.startTime == LocalTime.of(9, 0) } isBool true

      response.startTime isEqualTo LocalTime.of(9, 0)
      response.appointments.filter { ids.contains(it.id) }.all { it.startTime == LocalTime.of(13, 30) } isBool true
      response.appointments.filterNot { ids.contains(it.id) }.all { it.startTime == LocalTime.of(9, 0) } isBool true

      verify(outboundEventsService, times(9)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED), any(), eq(null))
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `updates end time`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentUpdateRequest(endTime = LocalTime.of(15, 0))
      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.endTime isEqualTo LocalTime.of(10, 30)
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.all { it.endTime == LocalTime.of(15, 0) } isBool true
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.all { it.endTime == LocalTime.of(10, 30) } isBool true

      response.endTime isEqualTo LocalTime.of(10, 30)
      response.appointments.filter { ids.contains(it.id) }.all { it.endTime == LocalTime.of(15, 0) } isBool true
      response.appointments.filterNot { ids.contains(it.id) }.all { it.endTime == LocalTime.of(10, 30) } isBool true

      verify(outboundEventsService, times(9)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED), any(), eq(null))
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `updates comment`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentUpdateRequest(extraInformation = "Updated appointment level comment")
      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.extraInformation isEqualTo "Appointment series level comment"
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.all { it.extraInformation == "Updated appointment level comment" } isBool true
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.all { it.extraInformation == "Appointment level comment" } isBool true

      response.extraInformation isEqualTo "Appointment series level comment"
      response.appointments.filter { ids.contains(it.id) }.all { it.extraInformation == "Updated appointment level comment" } isBool true
      response.appointments.filterNot { ids.contains(it.id) }.all { it.extraInformation == "Appointment level comment" } isBool true

      verify(outboundEventsService, times(9)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED), any(), eq(null))
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `adds prisoners to appointment`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      // C3456DE is already allocated
      val request = AppointmentUpdateRequest(addPrisonerNumbers = listOf("C3456DE", "D4567EF", "E5678FG"))
      val updated = LocalDateTime.now()
      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        mapOf(
          "C3456DE" to PrisonerSearchPrisonerFixture.instance(prisonerNumber = "C3456DE", bookingId = 3),
          "D4567EF" to PrisonerSearchPrisonerFixture.instance(prisonerNumber = "D4567EF", bookingId = 4),
          "E5678FG" to PrisonerSearchPrisonerFixture.instance(prisonerNumber = "E5678FG", bookingId = 5),
        ),
        updated,
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      with(appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.flatMap { it.attendees() }) {
        // No attendee record should be duplicated
        this hasSize appointmentsToUpdate.size * 5
        map { it.prisonerNumber }.distinct() isEqualTo listOf("A1234BC", "B2345CD", "C3456DE", "D4567EF", "E5678FG")
        filter { listOf("A1234BC", "B2345CD", "C3456DE").contains(it.prisonerNumber) }
          .onEach { it.addedTime isEqualTo null }
          .onEach { it.addedBy isEqualTo null }
        filter { listOf("D4567EF", "E5678FG").contains(it.prisonerNumber) }
          .onEach { it.addedTime isEqualTo updated }
          .onEach { it.addedBy isEqualTo updatedBy }
      }
      with(appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.flatMap { it.attendees() }) {
        map { it.prisonerNumber }.distinct() isEqualTo listOf("A1234BC", "B2345CD", "C3456DE")
        onEach { it.addedTime isEqualTo null }
        onEach { it.addedBy isEqualTo null }
      }

      with(response.appointments.filter { ids.contains(it.id) }.flatMap { it.attendees }) {
        // No attendee record should be duplicated
        this hasSize appointmentsToUpdate.size * 5
        map { it.prisonerNumber }.distinct() isEqualTo listOf("A1234BC", "B2345CD", "C3456DE", "D4567EF", "E5678FG")
        filter { listOf("A1234BC", "B2345CD", "C3456DE").contains(it.prisonerNumber) }
          .onEach { it.addedTime isEqualTo null }
          .onEach { it.addedBy isEqualTo null }
        filter { listOf("D4567EF", "E5678FG").contains(it.prisonerNumber) }
          .onEach { it.addedTime isEqualTo updated }
          .onEach { it.addedBy isEqualTo updatedBy }
      }
      with(response.appointments.filterNot { ids.contains(it.id) }.flatMap { it.attendees }) {
        map { it.prisonerNumber }.distinct() isEqualTo listOf("A1234BC", "B2345CD", "C3456DE")
        onEach { it.addedTime isEqualTo null }
        onEach { it.addedBy isEqualTo null }
      }

      verify(outboundEventsService, times(6)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_CREATED), any(), eq(null))
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `removes prisoners from appointment`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      // D4567EF is not an attendee
      val request = AppointmentUpdateRequest(removePrisonerNumbers = listOf("B2345CD", "C3456DE", "D4567EF"))
      val attendeesExpectedToBeRemoved = appointmentsToUpdate.flatMap { it.attendees() }.filter { listOf("B2345CD", "C3456DE").contains(it.prisonerNumber) }
      val updated = LocalDateTime.now()
      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        emptyMap(),
        updated,
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      attendeesExpectedToBeRemoved
        .onEach { it.removedTime isEqualTo updated }
        .onEach { it.removalReason isEqualTo permanentRemovalByUserAppointmentAttendeeRemovalReason }
        .onEach { it.removedBy isEqualTo updatedBy }

      with(appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.flatMap { it.attendees() }) {
        map { it.prisonerNumber }.distinct() isEqualTo listOf("A1234BC")
        onEach { it.removedTime isEqualTo null }
        onEach { it.removalReason isEqualTo null }
        onEach { it.removedBy isEqualTo null }
      }
      with(appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.flatMap { it.attendees() }) {
        map { it.prisonerNumber }.distinct() isEqualTo listOf("A1234BC", "B2345CD", "C3456DE")
        onEach { it.removedTime isEqualTo null }
        onEach { it.removalReason isEqualTo null }
        onEach { it.removedBy isEqualTo null }
      }

      with(response.appointments.filter { ids.contains(it.id) }.flatMap { it.attendees }) {
        map { it.prisonerNumber }.distinct() isEqualTo listOf("A1234BC")
        onEach { it.removedTime isEqualTo null }
        onEach { it.removalReasonId isEqualTo null }
        onEach { it.removedBy isEqualTo null }
      }
      with(response.appointments.filterNot { ids.contains(it.id) }.flatMap { it.attendees }) {
        map { it.prisonerNumber }.distinct() isEqualTo listOf("A1234BC", "B2345CD", "C3456DE")
        onEach { it.removedTime isEqualTo null }
        onEach { it.removalReasonId isEqualTo null }
        onEach { it.removedBy isEqualTo null }
      }

      verify(outboundEventsService, times(6)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_DELETED), any(), eq(null))
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `adds and removes prisoners on appointment`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentUpdateRequest(
        // C3456DE is being both removed and added. This will result in a soft deleted attendee record and a new attendee record for that prisoner with only the latter returned by the API
        removePrisonerNumbers = listOf("B2345CD", "C3456DE", "D4567EF"),
        addPrisonerNumbers = listOf("C3456DE", "D4567EF", "E5678FG"),
      )
      val attendeesExpectedToBeRemoved = appointmentsToUpdate.flatMap { it.attendees() }.filter { listOf("B2345CD", "C3456DE").contains(it.prisonerNumber) }
      val updated = LocalDateTime.now()
      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        mapOf(
          "C3456DE" to PrisonerSearchPrisonerFixture.instance(prisonerNumber = "C3456DE", bookingId = 3),
          "D4567EF" to PrisonerSearchPrisonerFixture.instance(prisonerNumber = "D4567EF", bookingId = 4),
          "E5678FG" to PrisonerSearchPrisonerFixture.instance(prisonerNumber = "E5678FG", bookingId = 5),
        ),
        updated,
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      attendeesExpectedToBeRemoved
        .onEach { it.removedTime isEqualTo updated }
        .onEach { it.removalReason isEqualTo permanentRemovalByUserAppointmentAttendeeRemovalReason }
        .onEach { it.removedBy isEqualTo updatedBy }

      with(appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.flatMap { it.attendees() }) {
        // No attendee record should be duplicated
        this hasSize appointmentsToUpdate.size * 4
        map { it.prisonerNumber }.distinct() isEqualTo listOf("A1234BC", "C3456DE", "D4567EF", "E5678FG")
        filter { listOf("A1234BC").contains(it.prisonerNumber) }
          .onEach { it.addedTime isEqualTo null }
          .onEach { it.addedBy isEqualTo null }
        filter { listOf("C3456DE", "D4567EF", "E5678FG").contains(it.prisonerNumber) }
          .onEach { it.addedTime isEqualTo updated }
          .onEach { it.addedBy isEqualTo updatedBy }
      }
      with(appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.flatMap { it.attendees() }) {
        map { it.prisonerNumber }.distinct() isEqualTo listOf("A1234BC", "B2345CD", "C3456DE")
        onEach { it.addedTime isEqualTo null }
        onEach { it.addedBy isEqualTo null }
      }

      with(response.appointments.filter { ids.contains(it.id) }.flatMap { it.attendees }) {
        // No attendee record should be duplicated
        this hasSize appointmentsToUpdate.size * 4
        map { it.prisonerNumber }.distinct() isEqualTo listOf("A1234BC", "C3456DE", "D4567EF", "E5678FG")
        filter { listOf("A1234BC").contains(it.prisonerNumber) }
          .onEach { it.addedTime isEqualTo null }
          .onEach { it.addedBy isEqualTo null }
        filter { listOf("C3456DE", "D4567EF", "E5678FG").contains(it.prisonerNumber) }
          .onEach { it.addedTime isEqualTo updated }
          .onEach { it.addedBy isEqualTo updatedBy }
      }
      with(response.appointments.filterNot { ids.contains(it.id) }.flatMap { it.attendees }) {
        map { it.prisonerNumber }.distinct() isEqualTo listOf("A1234BC", "B2345CD", "C3456DE")
        onEach { it.addedTime isEqualTo null }
        onEach { it.addedBy isEqualTo null }
      }

      verify(outboundEventsService, times(6)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_DELETED), any(), eq(null))
      verify(outboundEventsService, times(9)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_CREATED), any(), eq(null))
      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `sets updated and updated by on appointment series and appointment when property changed`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentUpdateRequest(internalLocationId = 456)
      val updated = LocalDateTime.now()
      val response = service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        emptyMap(),
        updated,
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.updatedTime isEqualTo updated
      appointmentSeries.updatedBy isEqualTo updatedBy
      with(appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }) {
        this.map { it.updatedTime }.distinct().single() isEqualTo updated
        this.map { it.updatedBy }.distinct().single() isEqualTo updatedBy
      }
      with(appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }) {
        this.map { it.updatedTime }.distinct().single() isEqualTo null
        this.map { it.updatedBy }.distinct().single() isEqualTo null
      }

      response.updatedTime isEqualTo updated
      response.updatedBy isEqualTo updatedBy
      with(response.appointments.filter { ids.contains(it.id) }) {
        this.map { it.updatedTime }.distinct().single() isEqualTo updated
        this.map { it.updatedBy }.distinct().single() isEqualTo updatedBy
      }
      with(response.appointments.filterNot { ids.contains(it.id) }) {
        this.map { it.updatedTime }.distinct().single() isEqualTo null
        this.map { it.updatedBy }.distinct().single() isEqualTo null
      }
    }

    @Test
    fun `track custom event using supplied counts and start time`() {
      val appointmentsToUpdate = applyToThis
      val request = AppointmentUpdateRequest(internalLocationId = 456)
      val startTimeInMs = System.currentTimeMillis()
      service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        appointmentsToUpdate.map { it.appointmentId }.toSet(),
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        1,
        3,
        startTimeInMs,
        trackEvent = true,
        auditEvent = false,
      )

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_EDITED.value), telemetryPropertyMap.capture(), telemetryMetricsMap.capture())

      with(telemetryMetricsMap.firstValue) {
        this[APPOINTMENT_COUNT_METRIC_KEY] isEqualTo 1.0
        this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY] isEqualTo 3.0
        assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isCloseTo((System.currentTimeMillis() - startTimeInMs).toDouble(), within(1000.0))
      }
    }

    @Test
    fun `do not track custom event`() {
      val appointmentsToUpdate = applyToThis
      val request = AppointmentUpdateRequest(internalLocationId = 456)
      val startTimeInMs = System.currentTimeMillis()
      service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        appointmentsToUpdate.map { it.appointmentId }.toSet(),
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        1,
        3,
        startTimeInMs,
        trackEvent = false,
        auditEvent = false,
      )

      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `track audit event`() {
      val appointmentsToUpdate = applyToThis
      val request = AppointmentUpdateRequest(internalLocationId = 456)
      service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        appointmentsToUpdate.map { it.appointmentId }.toSet(),
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = true,
      )

      verify(auditService).logEvent(any<AppointmentEditedEvent>())
    }

    @Test
    fun `do not track audit event`() {
      val appointmentsToUpdate = applyToThis
      val request = AppointmentUpdateRequest(internalLocationId = 456)
      service.updateAppointments(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        appointmentsToUpdate.map { it.appointmentId }.toSet(),
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      verifyNoInteractions(auditService)
    }
  }

  @Nested
  @DisplayName("instance count")
  inner class UpdateInstanceCount {
    @Test
    fun `no updates`() {
      val request = AppointmentUpdateRequest()
      val appointmentSeries = appointmentSeriesEntity()
      service.getUpdateInstancesCount(request, appointmentSeries.appointments()) isEqualTo 0
    }

    @Test
    fun `update category code`() {
      val request = AppointmentUpdateRequest(categoryCode = "NEW")
      service.getUpdateInstancesCount(
        request,
        applyToThis,
      ) isEqualTo applyToThis.flatMap { it.attendees() }.size
    }

    @Test
    fun `update location`() {
      val request = AppointmentUpdateRequest(internalLocationId = 456)
      service.getUpdateInstancesCount(
        request,
        applyToThisAndAllFuture,
      ) isEqualTo applyToThisAndAllFuture.flatMap { it.attendees() }.size
    }

    @Test
    fun `remove prisoners`() {
      // Only A1234BC is currently allocated
      val request = AppointmentUpdateRequest(removePrisonerNumbers = listOf("A1234BC", "D4567EF"))
      service.getUpdateInstancesCount(request, applyToAllFuture) isEqualTo applyToAllFuture.size
    }

    @Test
    fun `add prisoners`() {
      // C3456DE is already allocated
      val request = AppointmentUpdateRequest(addPrisonerNumbers = listOf("C3456DE", "D4567EF", "E5678FG"))
      service.getUpdateInstancesCount(request, applyToThis) isEqualTo applyToThis.size * 2
    }

    @Test
    fun `remove and add prisoners`() {
      // Only A1234BC is currently allocated
      val request = AppointmentUpdateRequest(
        removePrisonerNumbers = listOf("A1234BC"),
        addPrisonerNumbers = listOf("D4567EF", "E5678FG"),
      )
      service.getUpdateInstancesCount(
        request,
        applyToThis,
      ) isEqualTo applyToThis.size + applyToThis.size * 2
    }

    @Test
    fun `does not include removed prisoners when a property is also updated`() {
      val request = AppointmentUpdateRequest(
        startTime = LocalTime.of(8, 30),
        removePrisonerNumbers = listOf("A1234BC", "D4567EF"),
      )
      service.getUpdateInstancesCount(
        request,
        applyToThisAndAllFuture,
      ) isEqualTo applyToThisAndAllFuture.flatMap { it.attendees() }.size
    }

    @Test
    fun `includes added prisoners when a property is also updated`() {
      val request = AppointmentUpdateRequest(
        endTime = LocalTime.of(11, 0),
        addPrisonerNumbers = listOf("D4567EF", "E5678FG"),
      )
      service.getUpdateInstancesCount(
        request,
        applyToAllFuture,
      ) isEqualTo applyToAllFuture.flatMap { it.attendees() }.size + (applyToAllFuture.size * 2)
    }

    @Test
    fun `update a property, remove a prisoner and add two prisoners`() {
      val request = AppointmentUpdateRequest(
        extraInformation = "New",
        removePrisonerNumbers = listOf("A1234BC", "D4567EF"),
        addPrisonerNumbers = listOf("C3456DE", "D4567EF", "E5678FG"),
      )
      service.getUpdateInstancesCount(
        request,
        applyToAllFuture,
      ) isEqualTo applyToAllFuture.flatMap { it.attendees() }.size + (applyToAllFuture.size * 2)
    }
  }
}
