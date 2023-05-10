package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.EventReviewSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventReview as ModelEventReview

@Service
class EventReviewService(
  private val eventReviewRepository: EventReviewRepository,
  private val eventReviewSearchSpecification: EventReviewSearchSpecification,
) {
  fun getFilteredEvents(
    page: Int,
    size: Int,
    sortDirection: String,
    request: EventReviewSearchRequest,
  ): Page<ModelEventReview> {
    val sort: Sort? = createSort(sortDirection)
    val pageable: Pageable = if (sort != null) PageRequest.of(page, size, sort) else PageRequest.of(page, size)

    // Build a JPA search specification for the requested options
    var spec = eventReviewSearchSpecification.prisonCodeEquals(request.prisonCode)
    with(request) {
      prisonerNumber?.apply {
        spec = spec.and(eventReviewSearchSpecification.prisonerNumberEquals(prisonerNumber))
      }
      eventDate?.apply {
        spec = spec.and(
          eventReviewSearchSpecification.eventTimeBetween(
            eventDate.atStartOfDay(),
            eventDate.plusDays(1).atStartOfDay(),
          ),
        )
      }
      acknowledgedEvents?.apply {
        spec = if (this) {
          spec.and(eventReviewSearchSpecification.isAcknowledged())
        } else {
          spec.and(eventReviewSearchSpecification.isNotAcknowledged())
        }
      }
    }

    val results = eventReviewRepository.findAll(spec, pageable)

    return PageImpl(
      results.map { transform(it) }.toList(),
      pageable,
      results.totalElements,
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
