package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.MovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
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
  @Autowired
  private lateinit var appointmentAttendeeRepository: AppointmentAttendeeRepository

  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  private val activeInPrisoner = PrisonerSearchPrisonerFixture.instance(
    prisonerNumber = "A1234BC",
    inOutStatus = Prisoner.InOutStatus.IN,
    status = "ACTIVE IN",
    lastMovementType = null,
    confirmedReleaseDate = LocalDate.now().plusDays(2),
  )

  private val prisonerReleasedToday = PrisonerSearchPrisonerFixture.instance(
    prisonerNumber = "B2345CD",
    inOutStatus = Prisoner.InOutStatus.OUT,
    status = "INACTIVE OUT",
    lastMovementType = MovementType.RELEASE,
    confirmedReleaseDate = LocalDate.now(),
  )

  private val prisonerReleasedOneWeekAgo = PrisonerSearchPrisonerFixture.instance(
    prisonerNumber = "C3456DE",
    inOutStatus = Prisoner.InOutStatus.OUT,
    status = "INACTIVE OUT",
    lastMovementType = MovementType.RELEASE,
    confirmedReleaseDate = LocalDate.now().minusWeeks(1),
  )

  @BeforeEach
  fun setUp() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf("A1234BC", "B2345CD", "C3456DE"), listOf(activeInPrisoner, prisonerReleasedToday, prisonerReleasedOneWeekAgo))
  }

  @Sql("classpath:test_data/seed-manage-appointments-job.sql")
  @Test
  fun `remove attendees for released prisoners`() {
    with(appointmentAttendeeRepository.findAll()) {
      size isEqualTo 18
      onEach { it.isRemoved() isEqualTo false }
      onEach { it.isDeleted isEqualTo false }
    }

    webTestClient.manageAppointmentAttendees(1, 1)

    with(appointmentAttendeeRepository.findAll()) {
      size isEqualTo 11
      onEach { it.isRemoved() isEqualTo false }
      onEach { it.isDeleted isEqualTo false }
    }

    verify(eventsPublisher, times(7)).send(eventCaptor.capture())

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.deleted" }) {
      size isEqualTo 7
      Assertions.assertThat(map { it.additionalInformation }).containsExactlyElementsOf(
        listOf(6L, 8L, 9L, 17L, 18L, 11L, 12L).map { AppointmentInstanceInformation(it) },
      )
    }

    verifyNoMoreInteractions(eventsPublisher)
  }

  private fun WebTestClient.manageAppointmentAttendees(daysBeforeNow: Long, daysAfterNow: Long) {
    post()
      .uri("/job/appointments/manage-attendees?daysBeforeNow=$daysBeforeNow&daysAfterNow=$daysAfterNow")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isAccepted
    Thread.sleep(3000)
  }
}
