package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.MovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.daysAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.risleyPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledOnTransferEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentInstanceInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import java.time.LocalDate

@TestPropertySource(
  properties = [
    "feature.event.appointments.appointment-instance.created=true",
    "feature.event.appointments.appointment-instance.updated=true",
    "feature.event.appointments.appointment-instance.deleted=true",
    "feature.event.appointments.appointment-instance.cancelled=true",
  ],
)
class AppointmentJobIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @MockBean
  private lateinit var auditService: AuditService

  private val prisonNumber = "A1234BC"

  private val activeInPrisoner = PrisonerSearchPrisonerFixture.instance(
    prisonerNumber = "B2345CD",
    inOutStatus = Prisoner.InOutStatus.IN,
    status = "ACTIVE IN",
    prisonId = risleyPrisonCode,
    lastMovementType = null,
    confirmedReleaseDate = null,
  )

  private val activeInDifferentPrison = PrisonerSearchPrisonerFixture.instance(
    prisonerNumber = prisonNumber,
    inOutStatus = Prisoner.InOutStatus.IN,
    status = "ACTIVE IN",
    prisonId = moorlandPrisonCode,
    lastMovementType = null,
    confirmedReleaseDate = null,
  )

  private val activeOutPrisoner = PrisonerSearchPrisonerFixture.instance(
    prisonerNumber = prisonNumber,
    inOutStatus = Prisoner.InOutStatus.OUT,
    status = "ACTIVE OUT",
    prisonId = risleyPrisonCode,
    lastMovementType = null,
    confirmedReleaseDate = null,
  )

  private val prisonerReleasedToday = PrisonerSearchPrisonerFixture.instance(
    prisonerNumber = prisonNumber,
    inOutStatus = Prisoner.InOutStatus.OUT,
    status = "INACTIVE OUT",
    prisonId = null,
    lastMovementType = MovementType.RELEASE,
    confirmedReleaseDate = LocalDate.now(),
  )

  private val expiredMovement = movement(prisonerNumber = prisonNumber, fromPrisonCode = risleyPrisonCode, movementDate = 5.daysAgo())
  private val nonExpiredMovement = movement(prisonerNumber = prisonNumber, fromPrisonCode = risleyPrisonCode, movementDate = 4.daysAgo())

  @Sql("classpath:test_data/seed-manage-appointments-job.sql")
  @Test
  fun `do not remove released prisoner from future appointments when days from now does not include their appointments`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf("B2345CD"), listOf(activeInPrisoner))

    webTestClient.manageAppointmentAttendees(0)

    with(webTestClient.getAppointmentSeriesById(1)!!) {
      appointments.flatMap { it.attendees } hasSize 10
    }

    with(webTestClient.getAppointmentSetById(1)!!) {
      appointments.flatMap { it.attendees } hasSize 3
    }

    verifyNoInteractions(eventsPublisher)
    verifyNoInteractions(auditService)
  }

  @Sql("classpath:test_data/seed-manage-appointments-job.sql")
  @Test
  fun `remove released prisoner from future appointments`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf(prisonNumber, "B2345CD"), listOf(prisonerReleasedToday, activeInPrisoner))

    webTestClient.manageAppointmentAttendees(1)

    with(webTestClient.getAppointmentSeriesById(1)!!) {
      appointments.flatMap { it.attendees } hasSize 7
      appointments.single { it.id == 1L }.attendees.map { it.prisonerNumber } isEqualTo listOf(prisonNumber, "B2345CD")
      appointments.filterNot { it.id == 1L }.flatMap { it.attendees }.map { it.prisonerNumber }.toSet() isEqualTo setOf("B2345CD")
    }

    with(webTestClient.getAppointmentSetById(1)!!) {
      appointments.flatMap { it.attendees } hasSize 2
      appointments.flatMap { it.attendees }.map { it.prisonerNumber }.toSet() isEqualTo setOf("B2345CD", "C3456DE")
    }

    verify(eventsPublisher, times(4)).send(eventCaptor.capture())

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.deleted" }) {
      size isEqualTo 4
      assertThat(map { it.additionalInformation }).containsExactlyElementsOf(
        listOf(4L, 6L, 10L, 20L).map { AppointmentInstanceInformation(it) },
      )
    }

    verifyNoMoreInteractions(eventsPublisher)

    verify(auditService, times(4)).logEvent(any<AppointmentCancelledOnTransferEvent>())
  }

  @Sql("classpath:test_data/seed-manage-appointments-job.sql")
  @Test
  fun `do not remove transferred prisoner from future appointments when transfer was less than expired days ago`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf(prisonNumber, "B2345CD"), listOf(activeInDifferentPrison, activeInPrisoner))
    prisonApiMockServer.stubPrisonerMovements(listOf(prisonNumber), listOf(nonExpiredMovement))

    webTestClient.manageAppointmentAttendees(1)

    with(webTestClient.getAppointmentSeriesById(1)!!) {
      appointments.flatMap { it.attendees } hasSize 10
    }

    with(webTestClient.getAppointmentSetById(1)!!) {
      appointments.flatMap { it.attendees } hasSize 3
    }

    verifyNoInteractions(eventsPublisher)
    verifyNoInteractions(auditService)
  }

  @Sql("classpath:test_data/seed-manage-appointments-job.sql")
  @Test
  fun `remove transferred prisoner from future appointments`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf(prisonNumber, "B2345CD"), listOf(activeInDifferentPrison, activeInPrisoner))
    prisonApiMockServer.stubPrisonerMovements(listOf(prisonNumber), listOf(expiredMovement))

    webTestClient.manageAppointmentAttendees(1)

    with(webTestClient.getAppointmentSeriesById(1)!!) {
      appointments.flatMap { it.attendees } hasSize 7
      appointments.single { it.id == 1L }.attendees.map { it.prisonerNumber } isEqualTo listOf(prisonNumber, "B2345CD")
      appointments.filterNot { it.id == 1L }.flatMap { it.attendees }.map { it.prisonerNumber }.toSet() isEqualTo setOf("B2345CD")
    }

    with(webTestClient.getAppointmentSetById(1)!!) {
      appointments.flatMap { it.attendees } hasSize 2
      appointments.flatMap { it.attendees }.map { it.prisonerNumber }.toSet() isEqualTo setOf("B2345CD", "C3456DE")
    }

    verify(eventsPublisher, times(4)).send(eventCaptor.capture())

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.deleted" }) {
      size isEqualTo 4
      assertThat(map { it.additionalInformation }).containsExactlyElementsOf(
        listOf(4L, 6L, 10L, 20L).map { AppointmentInstanceInformation(it) },
      )
    }

    verifyNoMoreInteractions(eventsPublisher)

    verify(auditService, times(4)).logEvent(any<AppointmentCancelledOnTransferEvent>())
  }

  private fun WebTestClient.manageAppointmentAttendees(daysAfterNow: Long) {
    post()
      .uri("/job/appointments/manage-attendees?daysAfterNow=$daysAfterNow")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isAccepted
    Thread.sleep(3000)
  }

  private fun WebTestClient.getAppointmentSeriesById(id: Long) =
    get()
      .uri("/appointment-series/$id")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSeries::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getAppointmentSetById(id: Long) =
    get()
      .uri("/appointment-set/$id")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSet::class.java)
      .returnResult().responseBody
}
