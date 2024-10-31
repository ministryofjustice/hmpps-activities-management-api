package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentInstanceInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.event.appointments.appointment-instance.created=true",
    "feature.event.appointments.appointment-instance.deleted=true",
  ],
)
class MigrateAppointmentIntegrationTest : AppointmentsIntegrationTestBase() {

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @MockBean
  private lateinit var telemetryClient: TelemetryClient

  @MockBean
  private lateinit var auditService: AuditService
  private val auditableEventCaptor = argumentCaptor<AppointmentDeletedEvent>()

  @BeforeEach
  fun setUp() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers("A1234BC")
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers("B2345CD")
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers("C3456DE")

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes(
      listOf(
        appointmentCategoryReferenceCode("EDUC", "Education"),
      ),
    )

    prisonApiMockServer.stubGetLocationsForAppointments(
      "RSI",
      listOf(
        appointmentLocation(123, "RSI", userDescription = "Location 123"),
      ),
    )

    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
    )
  }

  @Test
  fun `migrate appointment forbidden`() {
    val request = appointmentMigrateRequest(categoryCode = "AC1")

    val error = webTestClient.post()
      .uri("/migrate-appointment")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isForbidden
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(error!!) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Access denied: Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `migrate appointment success`() {
    val request = appointmentMigrateRequest(categoryCode = "AC1")

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf(request.prisonerNumber!!),
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = request.prisonerNumber!!,
          bookingId = 1,
          prisonId = request.prisonCode!!,
        ),
      ),
    )

    val response = webTestClient.migrateAppointment(request)!!
    verifyAppointmentInstance(response)

    verifyNoInteractions(eventsPublisher)
    verifyNoInteractions(telemetryClient)
    verifyNoInteractions(auditService)
  }

  @Test
  fun `migrate appointment with comment over 40 characters success`() {
    val request = appointmentMigrateRequest(
      categoryCode = "AC1",
      comment = "First 40 characters will become the appointments custom name but the full comment will go to extra information.",
    )

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf(request.prisonerNumber!!),
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = request.prisonerNumber!!,
          bookingId = 1,
          prisonId = request.prisonCode!!,
        ),
      ),
    )

    val response = webTestClient.migrateAppointment(request)!!
    with(response) {
      assertThat(customName).isEqualTo("First 40 characters will become the appo")
      assertThat(extraInformation).isEqualTo(request.comment)
    }

    verifyNoInteractions(eventsPublisher)
    verifyNoInteractions(telemetryClient)
    verifyNoInteractions(auditService)
  }

  private fun verifyAppointmentInstance(response: AppointmentInstance) {
    with(response) {
      assertThat(id).isNotNull
      assertThat(appointmentSeriesId).isNotNull
      assertThat(appointmentId).isNotNull
      assertThat(appointmentAttendeeId).isNotNull
      assertThat(id).isEqualTo(appointmentAttendeeId)
      assertThat(createdBy).isEqualTo("CREATE.USER")
      assertThat(appointmentType).isEqualTo(AppointmentType.INDIVIDUAL)
      assertThat(prisonCode).isEqualTo("TPR")
      assertThat(prisonerNumber).isEqualTo("A1234BC")
      assertThat(bookingId).isEqualTo(123)
      assertThat(categoryCode).isEqualTo("AC1")
      assertThat(customName).isEqualTo("Appointment level comment")
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(inCell).isFalse
      assertThat(appointmentDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(startTime).isEqualTo(LocalTime.of(13, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(14, 30))
      assertThat(extraInformation).isNull()
      assertThat(createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo("CREATE.USER")
      assertThat(updatedTime).isNull()
      assertThat(updatedBy).isNull()
    }
  }

  @Test
  fun `delete migrated appointments - forbidden`() {
    val error = webTestClient.delete()
      .uri("/migrate-appointment/MDI?startDate=2023-09-25")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isForbidden
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(error!!) {
      assertThat(status).isEqualTo(403)
      assertThat(userMessage).isEqualTo("Access denied: Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
    }
  }

  @Sql(
    "classpath:test_data/seed-migrated-appointments.sql",
  )
  @Test
  fun `delete migrated appointments - success`() {
    webTestClient.deleteMigratedAppointments("RSI", LocalDate.now().plusDays(1))

    await untilAsserted {
      // Appointments starting earlier than supplied date should not have been deleted
      setOf(10L, 11L, 12L, 13L).forEach {
        webTestClient.getAppointmentDetailsById(it).isDeleted isBool false
      }
    }

    // Not migrated
    webTestClient.getAppointmentDetailsById(14).isDeleted isBool false
    // On start date
    webTestClient.getAppointmentDetailsById(15).isDeleted isBool true
    // Different prison
    webTestClient.getAppointmentDetailsById(16).isDeleted isBool false
    // On start date
    webTestClient.getAppointmentDetailsById(17).isDeleted isBool true

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.deleted")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).contains(
      AppointmentInstanceInformation(25),
      AppointmentInstanceInformation(27),
    )

    verifyNoInteractions(telemetryClient)

    verify(auditService, times(2)).logEvent(auditableEventCaptor.capture())
    assertThat(auditableEventCaptor.allValues.map { it.appointmentId }).contains(15, 17)
    verifyNoMoreInteractions(auditService)
  }

  @Sql(
    "classpath:test_data/seed-migrated-appointments.sql",
  )
  @Test
  fun `delete migrated chaplaincy appointments - success`() {
    webTestClient.deleteMigratedAppointments("RSI", LocalDate.now().plusDays(1), "CHAP")

    await untilAsserted {
      // Appointments starting earlier than supplied date should not have been deleted
      setOf(10L, 11L, 12L, 13L).forEach {
        webTestClient.getAppointmentDetailsById(it).isDeleted isBool false
      }
    }

    // Not migrated
    webTestClient.getAppointmentDetailsById(14).isDeleted isBool false
    // On start date with matching category code
    webTestClient.getAppointmentDetailsById(15).isDeleted isBool true
    // Different prison
    webTestClient.getAppointmentDetailsById(16).isDeleted isBool false
    // On start date with different category code
    webTestClient.getAppointmentDetailsById(17).isDeleted isBool false

    verify(eventsPublisher).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.deleted")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).contains(
      AppointmentInstanceInformation(25),
    )

    verifyNoInteractions(telemetryClient)

    verify(auditService, times(1)).logEvent(auditableEventCaptor.capture())
    assertThat(auditableEventCaptor.allValues.map { it.appointmentId }).contains(15)
    verifyNoMoreInteractions(auditService)
  }

  @Sql(
    "classpath:test_data/seed-migrated-deleted-appointments-removed-attendees.sql",
  )
  @Test
  fun `delete migrated appointments that have been subsequently cancelled, deleted or had their attendees removed - success`() {
    webTestClient.deleteMigratedAppointments("RSI", LocalDate.now().plusDays(1))

    await untilAsserted {
      // Appointments starting earlier than supplied date should not have been deleted
      setOf(10L, 11L, 12L, 13L).forEach {
        webTestClient.getAppointmentDetailsById(it).isDeleted isEqualTo true
      }
    }

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    // Deleted sync events should only be published for appointment instances not previously deleted
    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.deleted")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).contains(
      AppointmentInstanceInformation(20),
      AppointmentInstanceInformation(22),
    )

    verifyNoInteractions(telemetryClient)

    verify(auditService, times(3)).logEvent(auditableEventCaptor.capture())
    assertThat(auditableEventCaptor.allValues.map { it.appointmentId }).contains(10, 12, 13)
    verifyNoMoreInteractions(auditService)
  }

  private fun WebTestClient.migrateAppointment(
    request: AppointmentMigrateRequest,
  ) =
    post()
      .uri("/migrate-appointment")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentInstance::class.java)
      .returnResult().responseBody

  private fun WebTestClient.deleteMigratedAppointments(
    prisonCode: String,
    startDate: LocalDate,
    categoryCode: String? = null,
  ) {
    delete()
      .uri("/migrate-appointment/$prisonCode?startDate=$startDate" + (categoryCode?.let { "&categoryCode=$categoryCode" } ?: ""))
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
      .exchange()
      .expectStatus().isAccepted
  }
}
