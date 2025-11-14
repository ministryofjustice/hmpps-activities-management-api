package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto.UsageType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSeriesCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ORIGINAL_ID_PROPERTY_KEY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*

class AppointmentSeriesIntegrationTest : LocalStackTestBase() {
  @MockitoBean
  private lateinit var auditService: AuditService

  private val telemetryCaptor = argumentCaptor<Map<String, String>>()

  @Test
  fun `get appointment series authorisation required`() {
    webTestClient.get()
      .uri("/appointment-series/1")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get appointment series by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointment-series/-1")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `get single appointment series`() {
    val appointmentSeries = webTestClient.getAppointmentSeriesById(1)!!

    assertThat(appointmentSeries).isEqualTo(
      AppointmentSeries(
        1,
        AppointmentType.INDIVIDUAL,
        "TPR",
        "OIC",
        EventTier(
          id = 2,
          code = "TIER_2",
          description = "Tier 2",
        ),
        EventOrganiser(
          id = 1,
          code = "PRISON_STAFF",
          description = "Prison staff",
        ),
        "Appointment description",
        123,
        false,
        LocalDate.now().plusDays(1),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        null,
        "Appointment series level comment",
        "Prisoner series level comment",
        appointmentSeries.createdTime,
        "TEST.USER",
        null,
        null,
        appointments = listOf(
          Appointment(
            2,
            1,
            "TPR",
            "OIC",
            EventTier(
              id = 2,
              code = "TIER_2",
              description = "Tier 2",
            ),
            EventOrganiser(
              id = 1,
              code = "PRISON_STAFF",
              description = "Prison staff",
            ),
            "Appointment description",
            123,
            UUID.fromString("44444444-1111-2222-3333-444444444444"),
            false,
            LocalDate.now().plusDays(1),
            LocalTime.of(9, 0),
            LocalTime.of(10, 30),
            "Appointment level comment",
            "Prisoner level comment",
            appointmentSeries.createdTime,
            "TEST.USER",
            null,
            null,
            null,
            null,
            null,
            isDeleted = false,
            attendees = listOf(
              AppointmentAttendee(
                3,
                "A1234BC",
                456,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
              ),
            ),
          ),
        ),
        UUID.fromString("44444444-1111-2222-3333-444444444444"),
      ),
    )

    assertThat(appointmentSeries.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  @Test
  fun `get appointment series details authorisation required`() {
    webTestClient.get()
      .uri("/appointment-series/1/details")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get appointment series details by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointment-series/-1/details")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `get single appointment series details`() {
    val dpsLocation = dpsLocation(UUID.fromString("44444444-1111-2222-3333-444444444444"), "TPR")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "TPR",
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation.id, 123),
      ),
    )

    val appointmentDetails = webTestClient.getAppointmentSeriesDetailsById(1)!!

    assertThat(appointmentDetails).isEqualTo(
      AppointmentSeriesDetails(
        1,
        AppointmentType.INDIVIDUAL,
        "TPR",
        "Appointment description (Adjudication Hearing)",
        AppointmentCategorySummary("OIC", "Adjudication Hearing"),
        EventTier(
          id = 2,
          code = "TIER_2",
          description = "Tier 2",
        ),
        EventOrganiser(
          id = 1,
          code = "PRISON_STAFF",
          description = "Prison staff",
        ),
        "Appointment description",
        AppointmentLocationSummary(123, UUID.fromString("44444444-1111-2222-3333-444444444444"), "TPR", "User Description"),
        false,
        LocalDate.now().plusDays(1),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        null,
        "Appointment series level comment",
        appointmentDetails.createdTime,
        "TEST.USER",
        null,
        null,
        appointments = listOf(
          AppointmentSummary(
            2,
            1,
            LocalDate.now().plusDays(1),
            LocalTime.of(9, 0),
            LocalTime.of(10, 30),
            isEdited = false,
            isCancelled = false,
            isDeleted = false,
          ),
        ),
      ),
    )

    assertThat(appointmentDetails.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  @Test
  fun `create appointment series authorisation required`() {
    webTestClient.post()
      .uri("/appointment-series")
      .bodyValue(appointmentSeriesCreateRequest())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `create appointment series single appointment single prisoner success for internal location`() {
    val request = appointmentSeriesCreateRequest(categoryCode = "OIC", dpsLocationId = null)

    val dpsLocation = dpsLocation(UUID.fromString("44444444-1111-2222-3333-444444444444"), request.prisonCode!!, localName = "Test Appointment Location")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = request.prisonCode,
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation.id, request.internalLocationId!!),
      ),
    )

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = request.prisonerNumbers.first(),
          bookingId = 1,
          prisonId = request.prisonCode,
        ),
      ),
    )

    val dpsLocationId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    nomisMappingApiMockServer.stubMappingFromNomisId(123, dpsLocationId)

    val appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    val attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    assertSingleAppointmentSinglePrisoner(appointmentSeries, request.copy(dpsLocationId = dpsLocationId))
    assertSingleAppointmentSinglePrisoner(webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!, request.copy(dpsLocationId = dpsLocationId))

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, attendeeIds[0], "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo ""
    }
  }

  @Test
  fun `create appointment series single appointment single prisoner success for DPS Location ID`() {
    val request = appointmentSeriesCreateRequest(categoryCode = "OIC")

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = request.prisonerNumbers.first(),
          bookingId = 1,
          prisonId = request.prisonCode!!,
        ),
      ),
    )

    val dpsLocation = dpsLocation(request.dpsLocationId!!, "TPR", "ONE", "Location One")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "TPR",
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation.id, 1),
      ),
    )

    nomisMappingApiMockServer.stubMappingFromDpsUuid(request.dpsLocationId, 4445)

    val appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    val attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    assertSingleAppointmentSinglePrisoner(appointmentSeries, request.copy(internalLocationId = 4445))
    assertSingleAppointmentSinglePrisoner(webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!, request.copy(internalLocationId = 4445))

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, attendeeIds[0], "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo ""
    }
  }

  @Test
  fun `create appointment series single appointment single prisoner success for in cell`() {
    val request = appointmentSeriesCreateRequest(categoryCode = "OIC", internalLocationId = null, dpsLocationId = null, inCell = true)

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = request.prisonerNumbers.first(),
          bookingId = 1,
          prisonId = request.prisonCode!!,
        ),
      ),
    )

    val appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    val attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    assertSingleAppointmentSinglePrisoner(appointmentSeries, request)
    assertSingleAppointmentSinglePrisoner(webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!, request)

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, attendeeIds[0], "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo ""
    }
  }

  @Test
  fun `create appointment series group appointment two prisoner success`() {
    val request = appointmentSeriesCreateRequest(
      categoryCode = "OIC",
      appointmentType = AppointmentType.GROUP,
      prisonerNumbers = listOf("A12345BC", "B23456CE"),
    )

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "A12345BC",
          bookingId = 1,
          prisonId = request.prisonCode!!,
        ),
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "B23456CE",
          bookingId = 2,
          prisonId = request.prisonCode,
        ),
      ),
    )

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "TPR",
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation(request.dpsLocationId!!, "TPR", "ONE", "Location One")),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(request.dpsLocationId, request.internalLocationId!!),
      ),
    )

    nomisMappingApiMockServer.stubMappingFromDpsUuid(request.dpsLocationId, request.internalLocationId)

    val appointmentSeries = webTestClient.createAppointmentSeries(request)!!

    val attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    assertSingleAppointmentTwoPrisoner(appointmentSeries, request)
    assertSingleAppointmentTwoPrisoner(webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!, request)

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, attendeeIds[0], "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, attendeeIds[1], "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo ""
    }
  }

  @Test
  fun `create appointment series duplicated from an original appointment`() {
    val request = appointmentSeriesCreateRequest(categoryCode = "OIC", internalLocationId = null, dpsLocationId = null, inCell = true, originalAppointmentId = 789L)

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = request.prisonerNumbers.first(),
          bookingId = 1,
          prisonId = request.prisonCode!!,
        ),
      ),
    )

    val appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    val attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    assertSingleAppointmentSinglePrisoner(appointmentSeries, request)
    assertSingleAppointmentSinglePrisoner(webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!, request)

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, attendeeIds[0], "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo "789"
    }
  }

  @Test
  fun `create individual repeat appointment series success`() {
    val request =
      appointmentSeriesCreateRequest(categoryCode = "OIC", schedule = AppointmentSeriesSchedule(AppointmentFrequency.FORTNIGHTLY, 3))

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = request.prisonerNumbers.first(),
          bookingId = 1,
          prisonId = request.prisonCode,
        ),
      ),
    )

    val appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    val attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    assertThat(appointmentSeries.appointments).hasSize(3)

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, attendeeIds[0], "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, attendeeIds[1], "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, attendeeIds[2], "OIC"),
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo ""
    }
  }

  @Test
  fun `create appointment series synchronously success`() {
    // 5 prisoners with 2 appointments results in 10 appointment instances. Lower than the configured max-sync-appointment-instance-actions value
    // The resulting create appointment request will be synchronous, creating all appointments and attendees
    val prisonerNumberToBookingIdMap = (1L..5L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentSeriesCreateRequest(
      categoryCode = "OIC",
      appointmentType = AppointmentType.GROUP,
      prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(),
      schedule = AppointmentSeriesSchedule(AppointmentFrequency.DAILY, 2),
    )

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      prisonerNumberToBookingIdMap.map {
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = it.key,
          bookingId = it.value,
          prisonId = request.prisonCode,
        )
      },
    )

    val appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    val attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    appointmentSeries.appointments hasSize 2
    attendeeIds hasSize 10

    val expectedOutboundEvents = attendeeIds.map { ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, it, "OIC") }.toTypedArray()

    validateOutboundEvents(*expectedOutboundEvents)

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo ""
    }
  }

  @Test
  fun `create appointment series asynchronously success`() {
    // 3 prisoners with 4 appointments results in 12 appointment instances. Higher than the configured max-sync-appointment-instance-actions value
    // The resulting create appointment request will only create the first appointment and its attendees synchronously. The remaining
    // appointments and attendees will be created as an asynchronous job
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }

    val request = appointmentSeriesCreateRequest(
      categoryCode = "OIC",
      appointmentType = AppointmentType.GROUP,
      prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(),
      schedule = AppointmentSeriesSchedule(AppointmentFrequency.DAILY, 4),
    )

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      prisonerNumberToBookingIdMap.map {
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = it.key,
          bookingId = it.value,
          prisonId = request.prisonCode,
        )
      },
    )

    // Synchronous creation. First appointment and attendees only
    var appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    var attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }
    appointmentSeries.appointments hasSize 1
    attendeeIds hasSize 3

    // Wait for remaining appointments to be created
    Thread.sleep(1000)
    appointmentSeries = webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!
    attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }
    appointmentSeries.appointments hasSize 4
    attendeeIds hasSize 12

    val expectedOutboundEvents = attendeeIds.map { ExpectedOutboundEvent(APPOINTMENT_INSTANCE_CREATED, it, "OIC") }.toTypedArray()

    validateOutboundEvents(*expectedOutboundEvents)

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo ""
    }
  }

  private fun assertSingleAppointmentSinglePrisoner(appointmentSeries: AppointmentSeries, request: AppointmentSeriesCreateRequest) {
    assertThat(appointmentSeries).isEqualTo(
      AppointmentSeries(
        appointmentSeries.id,
        request.appointmentType!!,
        request.prisonCode!!,
        request.categoryCode!!,
        EventTier(
          id = appointmentSeries.tier!!.id,
          code = request.tierCode!!,
          description = appointmentSeries.tier.description,
        ),
        EventOrganiser(
          id = appointmentSeries.organiser!!.id,
          code = request.organiserCode!!,
          description = appointmentSeries.organiser.description,
        ),
        request.customName,
        request.internalLocationId,
        request.inCell,
        request.startDate!!,
        request.startTime!!,
        request.endTime,
        null,
        request.extraInformation,
        request.prisonerExtraInformation,
        appointmentSeries.createdTime,
        "test-client",
        null,
        null,
        appointments = listOf(
          Appointment(
            appointmentSeries.appointments.first().id,
            1,
            request.prisonCode,
            request.categoryCode,
            EventTier(
              id = appointmentSeries.tier.id,
              code = request.tierCode,
              description = appointmentSeries.tier.description,
            ),
            EventOrganiser(
              id = appointmentSeries.organiser.id,
              code = request.organiserCode,
              description = appointmentSeries.organiser.description,
            ),
            request.customName,
            request.internalLocationId,
            request.dpsLocationId,
            request.inCell,
            request.startDate,
            request.startTime,
            request.endTime,
            request.extraInformation,
            request.prisonerExtraInformation,
            appointmentSeries.createdTime,
            "test-client",
            null,
            null,
            null,
            null,
            null,
            isDeleted = false,
            attendees = listOf(
              AppointmentAttendee(
                appointmentSeries.appointments.first().attendees.first().id,
                request.prisonerNumbers.first(),
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
              ),
            ),
          ),
        ),
        request.dpsLocationId,
      ),
    )

    assertThat(appointmentSeries.id).isGreaterThan(0)
    assertThat(appointmentSeries.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(appointmentSeries.appointments.first().id).isGreaterThan(0)
    assertThat(appointmentSeries.appointments.first().attendees.first().id).isGreaterThan(0)
  }

  private fun assertSingleAppointmentTwoPrisoner(appointmentSeries: AppointmentSeries, request: AppointmentSeriesCreateRequest) {
    assertThat(appointmentSeries)
      .usingRecursiveComparison()
      .ignoringCollectionOrder().isEqualTo(
        AppointmentSeries(
          appointmentSeries.id,
          request.appointmentType!!,
          request.prisonCode!!,
          request.categoryCode!!,
          EventTier(
            id = appointmentSeries.tier!!.id,
            code = request.tierCode!!,
            description = appointmentSeries.tier.description,
          ),
          EventOrganiser(
            id = appointmentSeries.organiser!!.id,
            code = request.organiserCode!!,
            description = appointmentSeries.organiser.description,
          ),
          request.customName,
          request.internalLocationId,
          request.inCell,
          request.startDate!!,
          request.startTime!!,
          request.endTime,
          null,
          request.extraInformation,
          request.prisonerExtraInformation,
          appointmentSeries.createdTime,
          "test-client",
          null,
          null,
          appointments = listOf(
            Appointment(
              appointmentSeries.appointments.first().id,
              1,
              request.prisonCode,
              request.categoryCode,
              EventTier(
                id = appointmentSeries.tier.id,
                code = request.tierCode,
                description = appointmentSeries.tier.description,
              ),
              EventOrganiser(
                id = appointmentSeries.organiser.id,
                code = request.organiserCode,
                description = appointmentSeries.organiser.description,
              ),
              request.customName,
              request.internalLocationId,
              request.dpsLocationId,
              request.inCell,
              request.startDate,
              request.startTime,
              request.endTime,
              request.extraInformation,
              request.prisonerExtraInformation,
              appointmentSeries.createdTime,
              "test-client",
              null,
              null,
              null,
              null,
              null,
              isDeleted = false,
              attendees = listOf(
                AppointmentAttendee(id = 2, prisonerNumber = "B23456CE", bookingId = 2, null, null, null, null, null, null, null, null),
                AppointmentAttendee(id = 1, prisonerNumber = "A12345BC", bookingId = 1, null, null, null, null, null, null, null, null),
              ),
            ),
          ),
          request.dpsLocationId,
        ),
      )

    assertThat(appointmentSeries.id).isGreaterThan(0)
    assertThat(appointmentSeries.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(appointmentSeries.appointments.first().id).isGreaterThan(0)
  }

  private fun WebTestClient.getAppointmentSeriesById(id: Long) = get()
    .uri("/appointment-series/$id")
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AppointmentSeries::class.java)
    .returnResult().responseBody

  private fun WebTestClient.getAppointmentSeriesDetailsById(id: Long) = get()
    .uri("/appointment-series/$id/details")
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AppointmentSeriesDetails::class.java)
    .returnResult().responseBody

  private fun WebTestClient.createAppointmentSeries(
    request: AppointmentSeriesCreateRequest,
  ) = post()
    .uri("/appointment-series")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .header(CASELOAD_ID, request.prisonCode)
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AppointmentSeries::class.java)
    .returnResult().responseBody
}
