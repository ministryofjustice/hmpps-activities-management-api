package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.RetryApiServiceTest.Companion.mockServer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto.UsageType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentEditedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentUncancelledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUncancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_DELETED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_UNCANCELLED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_UPDATED
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_DATE_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_TIME_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*

class AppointmentIntegrationTest : LocalStackTestBase() {

  @MockitoBean
  private lateinit var auditService: AuditService

  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  @BeforeEach
  fun setUp() {
    mockServer.resetAll()
  }

  @Test
  fun `update appointment by unknown id returns 404 not found`() {
    webTestClient.patch()
      .uri("/appointments/-1")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .bodyValue(AppointmentUpdateRequest())
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `update single appointment`() {
    val request = AppointmentUpdateRequest(
      categoryCode = "VLB",
      tierCode = "TIER_2",
      organiserCode = "PRISON_STAFF",
      dpsLocationId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
      startDate = LocalDate.now().plusDays(3),
      startTime = LocalTime.of(13, 30),
      endTime = LocalTime.of(15, 0),
      extraInformation = "Updated Appointment level comment",
      applyTo = ApplyTo.THIS_APPOINTMENT,
    )

    val dpsLocation = dpsLocation(request.dpsLocationId!!, "TPR")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "TPR",
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(request.dpsLocationId, 456),
      ),
    )

    nomisMappingApiMockServer.stubMappingFromDpsUuid(request.dpsLocationId, 456)

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(request.dpsLocationId, dpsLocation)

    val appointmentSeries = webTestClient.updateAppointment(2, request)!!
    val appointmentIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    with(appointmentSeries) {
      assertThat(categoryCode).isEqualTo("OIC")
      assertThat(tier!!.code).isEqualTo("TIER_2")
      assertThat(organiser!!.code).isEqualTo("PRISON_STAFF")
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(dpsLocationId).isEqualTo(UUID.fromString("44444444-1111-2222-3333-444444444444"))
      assertThat(inCell).isFalse
      assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(extraInformation).isEqualTo("Appointment series level comment")
      assertThat(customName).isEqualTo("Appointment description")
      assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(updatedBy).isEqualTo("test-client")
      with(appointments.single()) {
        assertThat(categoryCode).isEqualTo(request.categoryCode)
        assertThat(tier!!.code).isEqualTo("TIER_2")
        assertThat(organiser!!.code).isEqualTo("PRISON_STAFF")
        assertThat(internalLocationId).isEqualTo(456)
        assertThat(dpsLocationId).isEqualTo(request.dpsLocationId)
        assertThat(inCell).isFalse
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(startTime).isEqualTo(request.startTime)
        assertThat(endTime).isEqualTo(request.endTime)
        assertThat(extraInformation).isEqualTo(request.extraInformation)
        assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(updatedBy).isEqualTo("test-client")
        with(attendees.single()) {
          assertThat(prisonerNumber).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(456)
        }
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UPDATED, appointmentIds.first(), "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  @Deprecated("SAA-2421: In future on DPS Location and not internal location will be used")
  fun `update single appointment when using location id`() {
    val request = AppointmentUpdateRequest(
      categoryCode = "VLB",
      tierCode = "TIER_2",
      organiserCode = "PRISON_STAFF",
      internalLocationId = 456,
      startDate = LocalDate.now().plusDays(3),
      startTime = LocalTime.of(13, 30),
      endTime = LocalTime.of(15, 0),
      extraInformation = "Updated Appointment level comment",
      applyTo = ApplyTo.THIS_APPOINTMENT,
    )

    val dpsLocation = dpsLocation(UUID.fromString("11111111-1111-1111-1111-111111111111"), "TPR")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "TPR",
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation.id, request.internalLocationId!!),
      ),
    )

    nomisMappingApiMockServer.stubMappingFromNomisId(456, dpsLocation.id)

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(dpsLocation.id, dpsLocation)

    val appointmentSeries = webTestClient.updateAppointment(2, request)!!
    val appointmentIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    with(appointmentSeries) {
      assertThat(categoryCode).isEqualTo("OIC")
      assertThat(tier!!.code).isEqualTo("TIER_2")
      assertThat(organiser!!.code).isEqualTo("PRISON_STAFF")
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(dpsLocationId).isEqualTo(UUID.fromString("44444444-1111-2222-3333-444444444444"))
      assertThat(inCell).isFalse
      assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(extraInformation).isEqualTo("Appointment series level comment")
      assertThat(customName).isEqualTo("Appointment description")
      assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(updatedBy).isEqualTo("test-client")
      with(appointments.single()) {
        assertThat(categoryCode).isEqualTo(request.categoryCode)
        assertThat(tier!!.code).isEqualTo("TIER_2")
        assertThat(organiser!!.code).isEqualTo("PRISON_STAFF")
        assertThat(internalLocationId).isEqualTo(request.internalLocationId)
        assertThat(dpsLocationId).isEqualTo(dpsLocation.id)
        assertThat(inCell).isFalse
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(startTime).isEqualTo(request.startTime)
        assertThat(endTime).isEqualTo(request.endTime)
        assertThat(extraInformation).isEqualTo(request.extraInformation)
        assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(updatedBy).isEqualTo("test-client")
        with(attendees.single()) {
          assertThat(prisonerNumber).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(456)
        }
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UPDATED, appointmentIds.first(), "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `update single appointment to in cell`() {
    val request = AppointmentUpdateRequest(
      categoryCode = "VLB",
      tierCode = "TIER_2",
      organiserCode = "PRISON_STAFF",
      inCell = true,
      startDate = LocalDate.now().plusDays(3),
      startTime = LocalTime.of(13, 30),
      endTime = LocalTime.of(15, 0),
      extraInformation = "Updated Appointment level comment",
      applyTo = ApplyTo.THIS_APPOINTMENT,
    )

    val appointmentSeries = webTestClient.updateAppointment(2, request)!!
    val appointmentIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    with(appointmentSeries) {
      assertThat(categoryCode).isEqualTo("OIC")
      assertThat(tier!!.code).isEqualTo("TIER_2")
      assertThat(organiser!!.code).isEqualTo("PRISON_STAFF")
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(dpsLocationId).isEqualTo(UUID.fromString("44444444-1111-2222-3333-444444444444"))
      assertThat(inCell).isFalse
      assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(extraInformation).isEqualTo("Appointment series level comment")
      assertThat(customName).isEqualTo("Appointment description")
      assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(updatedBy).isEqualTo("test-client")
      with(appointments.single()) {
        assertThat(categoryCode).isEqualTo(request.categoryCode)
        assertThat(tier!!.code).isEqualTo("TIER_2")
        assertThat(organiser!!.code).isEqualTo("PRISON_STAFF")
        assertThat(internalLocationId).isNull()
        assertThat(dpsLocationId).isNull()
        assertThat(inCell).isTrue()
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(startTime).isEqualTo(request.startTime)
        assertThat(endTime).isEqualTo(request.endTime)
        assertThat(extraInformation).isEqualTo(request.extraInformation)
        assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(updatedBy).isEqualTo("test-client")
        with(attendees.single()) {
          assertThat(prisonerNumber).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(456)
        }
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UPDATED, appointmentIds.first(), "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `cancel single appointment with a reason that does NOT trigger a soft delete`() {
    val request = AppointmentCancelRequest(
      applyTo = ApplyTo.THIS_APPOINTMENT,
      cancellationReasonId = 2,
    )

    val appointmentSeries = webTestClient.cancelAppointment(2, request)!!
    val appointmentIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    with(appointmentSeries) {
      with(appointments.single()) {
        assertThat(cancellationReasonId).isEqualTo(2)
        assertThat(cancelledBy).isEqualTo("test-client")
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, appointmentIds.first(), "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentCancelledEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `cancel single appointment with a reason that triggers a soft delete`() {
    val request = AppointmentCancelRequest(
      applyTo = ApplyTo.THIS_APPOINTMENT,
      cancellationReasonId = 1,
    )

    val originalAppointmentSeries = webTestClient.getAppointmentSeriesById(1)!!
    val appointmentIds = originalAppointmentSeries.appointments.filterNot { it.isDeleted }
      .flatMap { it.attendees.map { attendee -> attendee.id } }

    val appointmentSeries = webTestClient.cancelAppointment(2, request)!!

    assertThat(appointmentSeries.appointments.filterNot { it.isDeleted }).isEmpty()

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, appointmentIds.first(), "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentDeletedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-id-5.sql",
  )
  @Test
  fun `cancel group repeat appointment this and all future appointments with a reason that triggers a soft delete`() {
    val request = AppointmentCancelRequest(
      applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS,
      cancellationReasonId = 1,
    )

    val appointmentSeries = webTestClient.cancelAppointment(12, request)!!

    with(appointmentSeries.appointments.filterNot { it.isDeleted }) {
      assertThat(map { it.cancellationReasonId }.distinct().single()).isNull()
      assertThat(map { it.cancelledBy }.distinct().single()).isNull()
      assertThat(map { it.cancelledTime }.distinct().single()).isNull()
      assertThat(subList(2, size)).isEmpty()
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 24, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 25, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 26, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 27, "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentDeletedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-id-5.sql",
  )
  @Test
  fun `cancel group repeat appointment this and all future appointments with a reason that does NOT trigger a soft delete`() {
    val request = AppointmentCancelRequest(
      applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS,
      cancellationReasonId = 2,
    )

    val appointmentSeries = webTestClient.cancelAppointment(12, request)!!

    with(appointmentSeries) {
      with(appointments.subList(0, 2)) {
        assertThat(map { it.cancellationReasonId }.distinct().single()).isNull()
        assertThat(map { it.cancelledBy }.distinct().single()).isNull()
        assertThat(map { it.cancelledTime }.distinct().single()).isNull()
      }
      with(appointments.subList(2, appointments.size)) {
        assertThat(map { it.cancellationReasonId }.distinct().single()).isEqualTo(2)
        assertThat(map { it.cancelledBy }.distinct().single()).isEqualTo("test-client")
        assertThat(map { it.cancelledTime }.distinct().single()).isCloseTo(
          LocalDateTime.now(),
          within(60, ChronoUnit.SECONDS),
        )
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 24, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 25, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 26, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 27, "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentCancelledEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-cancelled-id-1.sql",
  )
  @Test
  fun `uncancel a single appointment`() {
    val request = AppointmentUncancelRequest(
      applyTo = ApplyTo.THIS_APPOINTMENT,
    )

    val appointmentSeries = webTestClient.uncancelAppointment(3, request)
      .expectBody(AppointmentSeries::class.java)
      .returnResult().responseBody!!

    appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    assertThat(appointmentSeries.appointments).hasSize(4)

    with(appointmentSeries.appointments) {
      single { it.id == 3L }.isCancelled() isEqualTo false
      filter { it.id != 3L }
        .forEach {
          it.isCancelled() isEqualTo true
        }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UNCANCELLED, 3, "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentUncancelledEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-cancelled-id-1.sql",
  )
  @Test
  fun `uncancel a group appointment - THIS_AND_ALL_FUTURE_APPOINTMENTS`() {
    val request = AppointmentUncancelRequest(
      applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS,
    )

    val appointmentSeries = webTestClient.uncancelAppointment(4, request)
      .expectBody(AppointmentSeries::class.java)
      .returnResult().responseBody!!

    assertThat(appointmentSeries.appointments).hasSize(4)

    with(appointmentSeries.appointments) {
      single { it.id == 3L }.isCancelled() isEqualTo true
      single { it.id == 6L }.isCancelled() isEqualTo true
      filterNot { it.id == 3L || it.id == 6L }
        .forEach {
          it.isCancelled() isEqualTo false
        }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UNCANCELLED, 4, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UNCANCELLED, 5, "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentUncancelledEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-cancelled-id-1.sql",
  )
  @Test
  fun `uncancel a group appointment - ALL_FUTURE_APPOINTMENTS`() {
    val request = AppointmentUncancelRequest(
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    val appointmentSeries = webTestClient.uncancelAppointment(4, request)
      .expectBody(AppointmentSeries::class.java)
      .returnResult().responseBody!!

    assertThat(appointmentSeries.appointments).hasSize(4)

    with(appointmentSeries.appointments) {
      single { it.id == 6L }.isCancelled() isEqualTo true
      filterNot { it.id == 6L }
        .forEach {
          it.isCancelled() isEqualTo false
        }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UNCANCELLED, 3, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UNCANCELLED, 4, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UNCANCELLED, 5, "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentUncancelledEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-cancelled-id-1.sql",
  )
  @Test
  fun `400 - uncancel an appointment 6 days ago`() {
    val request = AppointmentUncancelRequest(
      applyTo = ApplyTo.THIS_APPOINTMENT,
    )

    val response = webTestClient.uncancelAppointment(6, request)

    response
      .expectStatus().isBadRequest
      .expectBody().jsonPath("developerMessage").isEqualTo("Cannot uncancel an appointment more than 5 days ago")
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-12-instances-id-7.sql",
  )
  @Test
  fun `cancel large group repeat appointment location asynchronously success`() {
    // Seed appointment series has 4 appointments each with 3 attendees equalling 12 appointment instances. Cancelling all of them
    // affects more instances than the configured max-sync-appointment-instance-actions value. The service will therefore
    // cancel only the first affected appointment and its attendees synchronously. The remaining appointments and attendees
    // will be cancelled as an asynchronous job
    val appointmentId = 22L
    val request = AppointmentCancelRequest(
      cancellationReasonId = 2,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    val result = webTestClient.cancelAppointment(appointmentId, request)!!

    // Synchronous cancel. Cancel specified appointment only
    with(result.appointments) {
      single { it.id == appointmentId }.isCancelled() isEqualTo true
      filter { it.id != appointmentId }.map { it.isCancelled() }.distinct().single() isEqualTo false
    }

    await untilAsserted {
      val appointmentSeries = webTestClient.getAppointmentSeriesById(result.id)!!

      appointmentSeries.appointments.count { it.isCancelled() } isEqualTo 4

      validateOutboundEvents(
        ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 30, "OIC"),
        ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 31, "OIC"),
        ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 32, "OIC"),
        ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 33, "OIC"),
        ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 34, "OIC"),
        ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 35, "OIC"),
        ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 36, "OIC"),
        ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 37, "OIC"),
        ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 38, "OIC"),
        ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 39, "OIC"),
        ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 40, "OIC"),
        ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CANCELLED, 41, "OIC"),
      )
    }

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_CANCELLED.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    telemetryPropertyMap.allValues hasSize 1
    telemetryMetricsMap.allValues hasSize 1

    with(telemetryPropertyMap.firstValue) {
      assertThat(this[USER_PROPERTY_KEY]).isEqualTo("test-client")
      assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(this[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("7")
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentId.toString())
      assertThat(this[APPLY_TO_PROPERTY_KEY]).isEqualTo(request.applyTo.toString())
    }

    with(telemetryMetricsMap.firstValue) {
      assertThat(this[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(4.0)
      assertThat(this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(12.0)
      assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isNotNull
    }

    verify(auditService).logEvent(any<AppointmentCancelledEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-12-instances-id-7.sql",
  )
  @Test
  fun `delete large group repeat appointment location asynchronously success`() {
    // Seed appointment series has 4 appointments each with 3 attendees equalling 12 appointment instances. Deleting all of them
    // affects more instances than the configured max-sync-appointment-instance-actions value. The service will therefore
    // delete only the first affected appointment and its attendees synchronously. The remaining appointments and attendees
    // will be deleted as an asynchronous job
    val appointmentId = 22L
    val request = AppointmentCancelRequest(
      cancellationReasonId = 1,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    val result = webTestClient.cancelAppointment(appointmentId, request)!!

    // Synchronous delete. Delete specified appointment only
    with(result.appointments.filterNot { it.isDeleted }) {
      singleOrNull { it.id == appointmentId } isEqualTo null
      filter { it.id != appointmentId } hasSize 3
    }

    await untilAsserted {
      val appointmentSeries = webTestClient.getAppointmentSeriesById(result.id)!!
      appointmentSeries.appointments.filterNot { it.isDeleted } hasSize 0
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 36, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 37, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 38, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 30, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 31, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 32, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 33, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 34, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 35, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 39, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 40, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 41, "OIC"),
    )

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_DELETED.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    telemetryPropertyMap.allValues hasSize 1
    telemetryMetricsMap.allValues hasSize 1

    with(telemetryPropertyMap.firstValue) {
      assertThat(this[USER_PROPERTY_KEY]).isEqualTo("test-client")
      assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(this[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("7")
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentId.toString())
      assertThat(this[APPLY_TO_PROPERTY_KEY]).isEqualTo(request.applyTo.toString())
    }

    with(telemetryMetricsMap.firstValue) {
      assertThat(this[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(4.0)
      assertThat(this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(12.0)
      assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isNotNull
    }

    verify(auditService).logEvent(any<AppointmentDeletedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-id-5.sql",
  )
  @Test
  @Deprecated("SAA-2421: In future on DPS Location and not internal location will be used")
  fun `update group repeat appointment this and all future appointments when using location ids`() {
    val request = AppointmentUpdateRequest(
      categoryCode = "VLB",
      tierCode = "TIER_2",
      organiserCode = "PRISON_STAFF",
      internalLocationId = 456,
      startDate = LocalDate.now().plusDays(3),
      startTime = LocalTime.of(13, 30),
      endTime = LocalTime.of(15, 0),
      extraInformation = "Updated Appointment level comment",
      addPrisonerNumbers = listOf("B2345CD", "C3456DE"),
      removePrisonerNumbers = listOf("A1234BC"),
      applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS,
    )

    val dpsLocation = dpsLocation(UUID.fromString("11111111-1111-1111-1111-111111111111"), "TPR")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "TPR",
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation.id, request.internalLocationId!!),
      ),
    )

    nomisMappingApiMockServer.stubMappingFromNomisId(456, dpsLocation.id)

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(dpsLocation.id, dpsLocation)

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.addPrisonerNumbers!!,
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "B2345CD", bookingId = 457, prisonId = "TPR"),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "C3456DE", bookingId = 458, prisonId = "TPR"),
      ),
    )

    val appointmentSeries = webTestClient.updateAppointment(12, request)!!

    with(appointmentSeries) {
      assertThat(categoryCode).isEqualTo("OIC")
      assertThat(tier!!.code).isEqualTo("TIER_1")
      assertThat(organiser).isNull()
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(dpsLocationId).isEqualTo(UUID.fromString("44444444-1111-2222-3333-444444444444"))
      assertThat(inCell).isFalse
      assertThat(startDate).isEqualTo(LocalDate.now().minusDays(3))
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(extraInformation).isEqualTo("Appointment series level comment")
      assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(updatedBy).isEqualTo("test-client")
      assertThat(appointments[0].startDate).isEqualTo(LocalDate.now().minusDays(3))
      assertThat(appointments[1].startDate).isEqualTo(LocalDate.now().minusDays(3).plusWeeks(1))
      assertThat(appointments[2].startDate).isEqualTo(request.startDate)
      assertThat(appointments[3].startDate).isEqualTo(request.startDate!!.plusWeeks(1))

      val tier1Appointments = appointments.subList(0, 2)
      assertThat(tier1Appointments).hasSize(2)
      tier1Appointments.forEach {
        assertThat(it.categoryCode).isEqualTo("OIC")
        assertThat(it.tier!!.code).isEqualTo("TIER_1")
        assertThat(it.organiser).isNull()
        assertThat(it.internalLocationId).isEqualTo(123)
        assertThat(it.dpsLocationId).isEqualTo(UUID.fromString("44444444-1111-2222-3333-444444444444"))
        assertThat(it.inCell).isFalse
        assertThat(it.startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(it.endTime).isEqualTo(LocalTime.of(10, 30))
        assertThat(it.extraInformation).isEqualTo("Appointment level comment")
        assertThat(it.updatedTime).isNull()
        assertThat(it.updatedBy).isNull()

        assertThat(it.attendees)
          .extracting(AppointmentAttendee::prisonerNumber, AppointmentAttendee::bookingId)
          .containsOnly(
            tuple("A1234BC", 456L),
            tuple("B2345CD", 457L),
          )
      }

      val tier2Appointments = appointments.subList(2, appointments.size)
      assertThat(tier2Appointments).hasSize(2)
      tier2Appointments.forEach {
        assertThat(it.categoryCode).isEqualTo(request.categoryCode)
        assertThat(it.tier!!.code).isEqualTo("TIER_2")
        assertThat(it.organiser!!.code).isEqualTo("PRISON_STAFF")
        assertThat(it.internalLocationId).isEqualTo(request.internalLocationId)
        assertThat(it.dpsLocationId).isEqualTo(dpsLocation.id)
        assertThat(it.inCell).isFalse
        assertThat(it.startTime).isEqualTo(request.startTime)
        assertThat(it.endTime).isEqualTo(request.endTime)
        assertThat(it.extraInformation).isEqualTo("Updated Appointment level comment")
        assertThat(it.updatedTime).isCloseTo(
          LocalDateTime.now(),
          within(60, ChronoUnit.SECONDS),
        )
        assertThat(it.updatedBy).isEqualTo("test-client")

        assertThat(it.attendees)
          .extracting(AppointmentAttendee::prisonerNumber, AppointmentAttendee::bookingId)
          .containsOnly(
            tuple("C3456DE", 458L),
            tuple("B2345CD", 457L),
          )
      }
    }

    val expectedOutboundEvents = appointmentSeries.appointments.subList(2, appointmentSeries.appointments.size).flatMap {
      it.attendees.filter { attendee -> attendee.prisonerNumber == "C3456DE" }
        .map { attendee -> ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, attendee.id, "OIC") }
    }
      .toMutableList()
      .also {
        it.addAll(
          listOf(
            ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UPDATED, 25, "OIC"),
            ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UPDATED, 27, "OIC"),
            ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 24, "OIC"),
            ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 26, "OIC"),
          ),
        )
      }
      .toTypedArray()

    validateOutboundEvents(*expectedOutboundEvents)

    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-id-5.sql",
  )
  @Test
  fun `update group repeat appointment this and all future appointments`() {
    val request = AppointmentUpdateRequest(
      categoryCode = "VLB",
      tierCode = "TIER_2",
      organiserCode = "PRISON_STAFF",
      dpsLocationId = UUID.fromString("11111111-1111-1111-1111-11111111111"),
      startDate = LocalDate.now().plusDays(3),
      startTime = LocalTime.of(13, 30),
      endTime = LocalTime.of(15, 0),
      extraInformation = "Updated Appointment level comment",
      addPrisonerNumbers = listOf("B2345CD", "C3456DE"),
      removePrisonerNumbers = listOf("A1234BC"),
      applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS,
    )

    val dpsLocation = dpsLocation(request.dpsLocationId!!, "TPR")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "TPR",
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(request.dpsLocationId, 456),
      ),
    )

    nomisMappingApiMockServer.stubMappingFromDpsUuid(request.dpsLocationId, 456)

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(request.dpsLocationId, dpsLocation)

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.addPrisonerNumbers!!,
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "B2345CD", bookingId = 457, prisonId = "TPR"),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "C3456DE", bookingId = 458, prisonId = "TPR"),
      ),
    )

    val appointmentSeries = webTestClient.updateAppointment(12, request)!!

    with(appointmentSeries) {
      assertThat(categoryCode).isEqualTo("OIC")
      assertThat(tier!!.code).isEqualTo("TIER_1")
      assertThat(organiser).isNull()
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(dpsLocationId).isEqualTo(UUID.fromString("44444444-1111-2222-3333-444444444444"))
      assertThat(inCell).isFalse
      assertThat(startDate).isEqualTo(LocalDate.now().minusDays(3))
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(extraInformation).isEqualTo("Appointment series level comment")
      assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(updatedBy).isEqualTo("test-client")
      assertThat(appointments[0].startDate).isEqualTo(LocalDate.now().minusDays(3))
      assertThat(appointments[1].startDate).isEqualTo(LocalDate.now().minusDays(3).plusWeeks(1))
      assertThat(appointments[2].startDate).isEqualTo(request.startDate)
      assertThat(appointments[3].startDate).isEqualTo(request.startDate!!.plusWeeks(1))

      val tier1Appointments = appointments.subList(0, 2)
      assertThat(tier1Appointments).hasSize(2)
      tier1Appointments.forEach {
        assertThat(it.categoryCode).isEqualTo("OIC")
        assertThat(it.tier!!.code).isEqualTo("TIER_1")
        assertThat(it.organiser).isNull()
        assertThat(it.internalLocationId).isEqualTo(123)
        assertThat(it.dpsLocationId).isEqualTo(UUID.fromString("44444444-1111-2222-3333-444444444444"))
        assertThat(it.inCell).isFalse
        assertThat(it.startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(it.endTime).isEqualTo(LocalTime.of(10, 30))
        assertThat(it.extraInformation).isEqualTo("Appointment level comment")
        assertThat(it.updatedTime).isNull()
        assertThat(it.updatedBy).isNull()

        assertThat(it.attendees)
          .extracting(AppointmentAttendee::prisonerNumber, AppointmentAttendee::bookingId)
          .containsOnly(
            tuple("A1234BC", 456L),
            tuple("B2345CD", 457L),
          )
      }

      val tier2Appointments = appointments.subList(2, appointments.size)
      assertThat(tier2Appointments).hasSize(2)
      tier2Appointments.forEach {
        assertThat(it.categoryCode).isEqualTo(request.categoryCode)
        assertThat(it.tier!!.code).isEqualTo("TIER_2")
        assertThat(it.organiser!!.code).isEqualTo("PRISON_STAFF")
        assertThat(it.internalLocationId).isEqualTo(456)
        assertThat(it.dpsLocationId).isEqualTo(UUID.fromString("11111111-1111-1111-1111-11111111111"))
        assertThat(it.inCell).isFalse
        assertThat(it.startTime).isEqualTo(request.startTime)
        assertThat(it.endTime).isEqualTo(request.endTime)
        assertThat(it.extraInformation).isEqualTo("Updated Appointment level comment")
        assertThat(it.updatedTime).isCloseTo(
          LocalDateTime.now(),
          within(60, ChronoUnit.SECONDS),
        )
        assertThat(it.updatedBy).isEqualTo("test-client")

        assertThat(it.attendees)
          .extracting(AppointmentAttendee::prisonerNumber, AppointmentAttendee::bookingId)
          .containsOnly(
            tuple("C3456DE", 458L),
            tuple("B2345CD", 457L),
          )
      }
    }

    val expectedOutboundEvents = appointmentSeries.appointments.subList(2, appointmentSeries.appointments.size).flatMap {
      it.attendees.filter { attendee -> attendee.prisonerNumber == "C3456DE" }
        .map { attendee -> ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, attendee.id, "OIC") }
    }
      .toMutableList()
      .also {
        it.addAll(
          listOf(
            ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UPDATED, 25, "OIC"),
            ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UPDATED, 27, "OIC"),
            ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 24, "OIC"),
            ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 26, "OIC"),
          ),
        )
      }
      .toTypedArray()

    validateOutboundEvents(*expectedOutboundEvents)

    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-12-instances-id-7.sql",
  )
  @Test
  fun `update large group repeat appointment location asynchronously success`() {
    // Seed appointment series has 4 appointments each with 3 attendees equalling 12 appointment instances. Editing all of them
    // affects more instances than the configured max-sync-appointment-instance-actions value. The service will therefore
    // update only the first affected appointment and its attendees synchronously. The remaining appointments and attendees
    // will be updated as an asynchronous job
    val appointmentId = 22L
    val request = AppointmentUpdateRequest(
      dpsLocationId = UUID.fromString("11111111-1111-1111-1111-11111111111"),
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    val dpsLocation = dpsLocation(request.dpsLocationId!!, "TPR")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "TPR",
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation.id, 456),
      ),
    )

    nomisMappingApiMockServer.stubMappingFromDpsUuid(dpsLocation.id, 456)

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(dpsLocation.id, dpsLocation)

    val result = webTestClient.updateAppointment(appointmentId, request)!!

    // Synchronous update. Update specified appointment only
    with(result.appointments) {
      single { it.id == appointmentId }.internalLocationId isEqualTo 456
      single { it.id == appointmentId }.dpsLocationId isEqualTo request.dpsLocationId
      filter { it.id != appointmentId }.map { it.internalLocationId }.distinct().single() isEqualTo 123
      filter { it.id != appointmentId }.map { it.dpsLocationId }.distinct().single() isEqualTo UUID.fromString("44444444-1111-2222-3333-444444444444")
    }

    await untilAsserted {
      val appointmentSeries = webTestClient.getAppointmentSeriesById(result.id)!!

      appointmentSeries.appointments.count { it.internalLocationId == 456L && it.dpsLocationId == request.dpsLocationId } isEqualTo 4

      val expectedOutboundEvents = appointmentSeries.appointments.flatMap { it.attendees }
        .map { ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UPDATED, it.id, "OIC") }
        .toTypedArray()

      validateOutboundEvents(*expectedOutboundEvents)
    }

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_EDITED.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    telemetryPropertyMap.allValues hasSize 1
    telemetryMetricsMap.allValues hasSize 1

    with(telemetryPropertyMap.firstValue) {
      assertThat(this[USER_PROPERTY_KEY]).isEqualTo("test-client")
      assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(this[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("7")
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentId.toString())
      assertThat(this[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("true")
      assertThat(this[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[APPLY_TO_PROPERTY_KEY]).isEqualTo(request.applyTo.toString())
    }

    with(telemetryMetricsMap.firstValue) {
      assertThat(this[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
      assertThat(this[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
      assertThat(this[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(4.0)
      assertThat(this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(12.0)
      assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isNotNull
    }

    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-12-instances-id-7.sql",
  )
  @Test
  @Deprecated("SAA-2421: In future on DPS Location and not internal location will be used")
  fun `update large group repeat appointment location asynchronously success when using location ids`() {
    // Seed appointment series has 4 appointments each with 3 attendees equalling 12 appointment instances. Editing all of them
    // affects more instances than the configured max-sync-appointment-instance-actions value. The service will therefore
    // update only the first affected appointment and its attendees synchronously. The remaining appointments and attendees
    // will be updated as an asynchronous job
    val appointmentId = 22L
    val request = AppointmentUpdateRequest(
      internalLocationId = 456,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    val dpsLocation = dpsLocation(UUID.fromString("11111111-1111-1111-1111-111111111111"), "TPR")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "TPR",
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation.id, request.internalLocationId!!),
      ),
    )

    nomisMappingApiMockServer.stubMappingFromNomisId(456, dpsLocation.id)

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(dpsLocation.id, dpsLocation)

    val result = webTestClient.updateAppointment(appointmentId, request)!!

    // Synchronous update. Update specified appointment only
    with(result.appointments) {
      single { it.id == appointmentId }.internalLocationId isEqualTo request.internalLocationId
      single { it.id == appointmentId }.dpsLocationId isEqualTo dpsLocation.id
      filter { it.id != appointmentId }.map { it.internalLocationId }.distinct().single() isEqualTo 123
      filter { it.id != appointmentId }.map { it.dpsLocationId }.distinct().single() isEqualTo UUID.fromString("44444444-1111-2222-3333-444444444444")
    }

    await untilAsserted {
      val appointmentSeries = webTestClient.getAppointmentSeriesById(result.id)!!
      appointmentSeries.appointments.count { it.internalLocationId == request.internalLocationId && it.dpsLocationId == dpsLocation.id } isEqualTo 4

      val expectedOutboundEvents = appointmentSeries.appointments.flatMap { it.attendees }
        .map { ExpectedOutboundEvent(APPOINTMENT_INSTANCE_UPDATED, it.id, "OIC") }
        .toTypedArray()

      validateOutboundEvents(*expectedOutboundEvents)
    }

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_EDITED.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    telemetryPropertyMap.allValues hasSize 1
    telemetryMetricsMap.allValues hasSize 1

    with(telemetryPropertyMap.firstValue) {
      assertThat(this[USER_PROPERTY_KEY]).isEqualTo("test-client")
      assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(this[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("7")
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentId.toString())
      assertThat(this[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("true")
      assertThat(this[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[APPLY_TO_PROPERTY_KEY]).isEqualTo(request.applyTo.toString())
    }

    with(telemetryMetricsMap.firstValue) {
      assertThat(this[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
      assertThat(this[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
      assertThat(this[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(4.0)
      assertThat(this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(12.0)
      assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isNotNull
    }

    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-12-instances-id-7.sql",
  )
  @Test
  fun `update large group repeat appointment attendees asynchronously success`() {
    // Seed appointment series has 4 appointments. Removing one prisoner and adding two new prisoners to all of them removes and adds
    // more attendees than the configured max-sync-appointment-instance-actions value. The service will therefore remove and
    // add attendees on only the first affected appointment and its attendees synchronously. The remaining appointments
    // will have attendees removed and added as an asynchronous job
    val appointmentId = 22L
    val request = AppointmentUpdateRequest(
      removePrisonerNumbers = listOf("A1234BC"),
      addPrisonerNumbers = listOf("D4567EF", "E5679FG"),
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.addPrisonerNumbers!!,
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "D4567EF", bookingId = 459, prisonId = "TPR"),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "E5679FG", bookingId = 460, prisonId = "TPR"),
      ),
    )

    val result = webTestClient.updateAppointment(appointmentId, request)!!

    // Synchronous update. Update specified appointment only
    with(result.appointments) {
      assertThat(single { it.id == appointmentId }.attendees.map { it.prisonerNumber }).containsOnly("B2345CD", "C3456DE", "D4567EF", "E5679FG")
      assertThat(filter { it.id != appointmentId }.flatMap { it.attendees }.map { it.prisonerNumber }.distinct()).containsOnly("A1234BC", "B2345CD", "C3456DE")
    }

    await untilAsserted {
      val appointmentSeries = webTestClient.getAppointmentSeriesById(result.id)!!
      assertThat(
        appointmentSeries.appointments.flatMap { it.attendees }.map { it.prisonerNumber }
          .distinct(),
      ).containsOnly("B2345CD", "C3456DE", "D4567EF", "E5679FG")
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, 1, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, 2, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, 3, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, 4, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, 5, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, 6, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, 7, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, 8, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 30, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 33, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 36, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 39, "OIC"),
    )

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_EDITED.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    telemetryPropertyMap.allValues hasSize 1
    telemetryMetricsMap.allValues hasSize 1

    with(telemetryPropertyMap.firstValue) {
      assertThat(this[USER_PROPERTY_KEY]).isEqualTo("test-client")
      assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(this[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("7")
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentId.toString())
      assertThat(this[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[APPLY_TO_PROPERTY_KEY]).isEqualTo(request.applyTo.toString())
    }

    with(telemetryMetricsMap.firstValue) {
      assertThat(this[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(this[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(2.0)
      assertThat(this[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(4.0)
      assertThat(this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(12.0)
      assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isNotNull
    }

    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  private fun WebTestClient.getAppointmentSeriesById(id: Long) = get()
    .uri("/appointment-series/$id")
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AppointmentSeries::class.java)
    .returnResult().responseBody

  private fun WebTestClient.updateAppointment(
    id: Long,
    request: AppointmentUpdateRequest,
  ) = patch()
    .uri("/appointments/$id")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isAccepted
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AppointmentSeries::class.java)
    .returnResult().responseBody

  private fun WebTestClient.cancelAppointment(
    id: Long,
    request: AppointmentCancelRequest,
  ) = put()
    .uri("/appointments/$id/cancel")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isAccepted
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AppointmentSeries::class.java)
    .returnResult().responseBody

  private fun WebTestClient.uncancelAppointment(
    id: Long,
    request: AppointmentUncancelRequest,
  ) = put()
    .uri("/appointments/$id/uncancel")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
}
