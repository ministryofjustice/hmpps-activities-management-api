package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.EventAcknowledgeRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.EventReviewSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.EventReviewSearchResults
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.EventReviewService
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [EventReviewController::class])
@ContextConfiguration(classes = [EventReviewController::class])
class EventReviewControllerTest : ControllerTestBase<EventReviewController>() {

  @MockitoBean
  private lateinit var eventReviewService: EventReviewService

  override fun controller() = EventReviewController(eventReviewService)

  private var prisonCode = "MDI"
  private var page = 0
  private var size = 10
  private var date = LocalDate.now()
  private var sort = "ascending"
  private var request = EventReviewSearchRequest(prisonCode, LocalDate.now())

  @Test
  fun `Success - 200 response`() {
    val response = buildResponse()
    whenever(eventReviewService.getFilteredEvents(page, size, sort, request)).thenReturn(response)

    val result = mockMvc.getEventsForReview(date, prisonCode, page, size, sort)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(result.contentAsString).isEqualTo(
      mapper.writeValueAsString(
        EventReviewSearchResults(response.content, response.number, response.totalElements, response.totalPages),
      ),
    )

    verify(eventReviewService).getFilteredEvents(0, 10, "ascending", request)
  }

  @Test
  fun `bad request - incorrect parameters`() {
    whenever(eventReviewService.getFilteredEvents(page, size, sort, request)).thenThrow(IllegalArgumentException("Wrong args"))

    val result = mockMvc.getEventsForReview(date, prisonCode, page, size, sort)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isBadRequest() } }
      .andReturn().response

    assertThat(result.contentAsString).contains("Wrong args")
  }

  @Test
  fun `success no data - 200 ok empty`() {
    val response = buildEmptyResponse()
    whenever(eventReviewService.getFilteredEvents(page, size, sort, request)).thenReturn(response)

    val result = mockMvc.getEventsForReview(date, prisonCode, page, size, sort)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(result.contentAsString).isEqualTo(
      mapper.writeValueAsString(
        EventReviewSearchResults(response.content, response.number, response.totalElements, response.totalPages),
      ),
    )

    verify(eventReviewService).getFilteredEvents(0, 10, "ascending", request)
  }

  @Test
  fun `acknowledgeEvents - 204 OK`() {
    val request = EventAcknowledgeRequest(eventReviewIds = listOf(1, 2, 3))

    val mockPrincipal: Principal = mock()
    whenever(mockPrincipal.name).thenReturn("THE USER NAME")

    mockMvc.acknowledgeEvents(prisonCode, request).andExpect {
      status {
        isNoContent()
      }
    }

    verify(eventReviewService).acknowledgeEvents(prisonCode, request, "USERNAME")
  }

  private fun MockMvc.getEventsForReview(date: LocalDate, prisonCode: String, page: Int, size: Int, sort: String = "ascending") =
    get("/event-review/prison/{prisonCode}?date={date}&page={page}&size={size}&sort={sort}", prisonCode, date, page, size, sort)

  private fun MockMvc.acknowledgeEvents(prison: String, request: EventAcknowledgeRequest) =
    post("/event-review/prison/{prison}/acknowledge", prison) {
      principal = Principal { "USERNAME" }
      content = mapper.writeValueAsString(request)
      contentType = MediaType.APPLICATION_JSON
    }

  private fun buildEmptyResponse(): Page<EventReview> = Page.empty()

  private fun buildResponse(): Page<EventReview> {
    val sort: Sort? = createSort(sort)
    val pageable: Pageable = if (sort != null) PageRequest.of(page, size, sort) else PageRequest.of(page, size)
    return PageImpl(
      listOf(
        EventReview(1, "service", "x.y.z", LocalDateTime.now(), prisonCode, "G1234FF", 1, "XYZ"),
        EventReview(1, "service", "x.y.z", LocalDateTime.now(), prisonCode, "G1234FF", 1, "XYZ"),
        EventReview(1, "service", "x.y.z", LocalDateTime.now(), prisonCode, "G1234FF", 1, "XYZ"),
      ),
      pageable,
      3,
    )
  }

  private fun createSort(sortDirection: String, sortField: String = "eventTime"): Sort? {
    return when (sortDirection) {
      "ascending" -> Sort.by(sortField).ascending()
      "descending" -> Sort.by(sortField).descending()
      else -> null
    }
  }
}
