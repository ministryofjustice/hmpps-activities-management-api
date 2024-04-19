package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReviewDescription
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventDescription
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.EventAcknowledgeRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.EventReviewSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventReview as ModelEventReview

@ExtendWith(FakeSecurityContext::class)
class EventReviewServiceTest {
  private val eventReviewRepository = mock<EventReviewRepository>()
  private val eventReviewSearchSpecification: EventReviewSearchSpecification = spy()
  private val telemetryClient: TelemetryClient = mock()
  private val eventReviewService = EventReviewService(eventReviewRepository, eventReviewSearchSpecification, telemetryClient)

  @Test
  fun `returns rows based on a search specification`() {
    val page = 0
    val size = 10
    val sortDirection = "ascending"
    val prisonCode = "MDI"
    val prisonerNumber = "G1234FF"
    val date = LocalDate.now()

    val repositoryResult = PageImpl(
      listOf(
        EventReview(1, "service", "x.y.z", LocalDateTime.now(), prisonCode, "G1234FF", 1, "XYZ"),
        EventReview(2, "service", "x.y.z", LocalDateTime.now(), prisonCode, "G1234FF", 1, "XYZ"),
        EventReview(3, "service", "x.y.z", LocalDateTime.now(), prisonCode, "G1234FF", 1, "XYZ"),
      ),
    )

    whenever(eventReviewRepository.findAll(any<Specification<EventReview>>(), any<Pageable>())).thenReturn(repositoryResult)

    val searchSpec = EventReviewSearchRequest(prisonCode = prisonCode, eventDate = date, prisonerNumber = prisonerNumber, acknowledgedEvents = true)

    val result = eventReviewService.getFilteredEvents(page, size, sortDirection, searchSpec)

    assertThat(result.totalElements).isEqualTo(3)
    assertThat(result.content).hasSize(3)
    assertThat(result.totalPages).isEqualTo(1)

    result.content.map {
      assertThat(it.eventReviewId in 1..3).isTrue
      assertThat(it.prisonerNumber).isEqualTo("G1234FF")
      assertThat(it.bookingId).isEqualTo(1)
      assertThat(it.eventData).isEqualTo("XYZ")
      assertThat(it.eventDescription).isEqualTo(null)
    }
  }

  @Test
  fun `returns rows based on a search specification with differing event descriptions`() {
    val page = 0
    val size = 10
    val sortDirection = "ascending"
    val prisonCode = "MDI"
    val prisonerNumber = "G1234FF"
    val date = LocalDate.now()

    val eventTime = LocalDateTime.now()
    val repositoryResult = PageImpl(
      listOf(
        EventReview(1, "service1", "prison-offender-events.prisoner.activities-changed", eventTime, prisonCode, "G1234FF", 1, "XYZ", eventDescription = EventReviewDescription.ACTIVITY_ENDED),
        EventReview(2, "service2", "prison-offender-events.prisoner.activities-changed", eventTime, prisonCode, "G1234FX", 2, "XYZ", eventDescription = EventReviewDescription.ACTIVITY_SUSPENDED),
        EventReview(3, "service3", "prisoner-offender-search.prisoner.released", eventTime, prisonCode, "G1234FY", 3, "XYZ", eventDescription = EventReviewDescription.TEMPORARY_RELEASE),
      ),
    )

    whenever(eventReviewRepository.findAll(any<Specification<EventReview>>(), any<Pageable>())).thenReturn(repositoryResult)

    val searchSpec = EventReviewSearchRequest(prisonCode = prisonCode, eventDate = date, prisonerNumber = prisonerNumber, acknowledgedEvents = true)

    val result = eventReviewService.getFilteredEvents(page, size, sortDirection, searchSpec)

    assertThat(result.totalElements).isEqualTo(3)
    assertThat(result.content).hasSize(3)
    assertThat(result.totalPages).isEqualTo(1)

    assertThat(result.content).isEqualTo(
      listOf(
        ModelEventReview(
          eventReviewId = 1,
          serviceIdentifier = "service1",
          eventType = "prison-offender-events.prisoner.activities-changed",
          eventData = "XYZ",
          eventTime = eventTime,
          bookingId = 1,
          prisonerNumber = "G1234FF",
          prisonCode = "MDI",
          eventDescription = EventDescription.ACTIVITY_ENDED,
        ),
        ModelEventReview(
          eventReviewId = 2,
          serviceIdentifier = "service2",
          eventType = "prison-offender-events.prisoner.activities-changed",
          eventData = "XYZ",
          eventTime = eventTime,
          bookingId = 2,
          prisonerNumber = "G1234FX",
          prisonCode = "MDI",
          eventDescription = EventDescription.ACTIVITY_SUSPENDED,
        ),
        ModelEventReview(
          eventReviewId = 3,
          serviceIdentifier = "service3",
          eventType = "prisoner-offender-search.prisoner.released",
          eventData = "XYZ",
          eventTime = eventTime,
          bookingId = 3,
          prisonerNumber = "G1234FY",
          prisonCode = "MDI",
          eventDescription = EventDescription.TEMPORARY_RELEASE,
        ),
      ),
    )
  }

  @Test
  fun `acknowledges events`() {
    val prisonCode = "MDI"
    val eventReviewIds = mutableListOf<Long>(1, 2, 3)
    val request = EventAcknowledgeRequest(eventReviewIds)
    val results = listOf(
      EventReview(1, "service", "x.y.z", LocalDateTime.now(), prisonCode, "G1234FF", 1, "XYZ"),
      EventReview(2, "service", "x.y.z", LocalDateTime.now(), prisonCode, "G1234FF", 1, "XYZ"),
      EventReview(3, "service", "x.y.z", LocalDateTime.now(), prisonCode, "G1234FF", 1, "XYZ"),
    )

    whenever(eventReviewRepository.findAllById(eventReviewIds)).thenReturn(results)

    eventReviewService.acknowledgeEvents(prisonCode, request, "PRINCIPAL")

    verify(eventReviewRepository).saveAllAndFlush(results)
  }
}
