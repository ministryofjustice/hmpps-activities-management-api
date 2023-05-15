package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.EventReviewSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(FakeSecurityContext::class)
class EventReviewServiceTest {
  private val eventReviewRepository = mock<EventReviewRepository>()
  private val eventReviewSearchSpecification: EventReviewSearchSpecification = spy()
  private val eventReviewService = EventReviewService(eventReviewRepository, eventReviewSearchSpecification)

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
    }
  }
}
