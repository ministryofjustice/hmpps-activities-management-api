package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.EventAcknowledgeRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.EventReviewSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACKNOWLEDGED_BY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACKNOWLEDGED_TIME_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_REVIEW_IDS_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.NUMBER_OF_RESULTS_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventReview as ModelEventReview

@Service
@Transactional(readOnly = true)
class EventReviewService(
  private val eventReviewRepository: EventReviewRepository,
  private val eventReviewSearchSpecification: EventReviewSearchSpecification,
  private val telemetryClient: TelemetryClient,
) {
  fun getFilteredEvents(
    page: Int,
    size: Int,
    sortDirection: String,
    request: EventReviewSearchRequest,
  ): Page<ModelEventReview> {
    val sort: Sort? = createSort(sortDirection)
    val pageable: Pageable = if (sort != null) PageRequest.of(page, size, sort) else PageRequest.of(page, size)
    var spec = eventReviewSearchSpecification.prisonCodeEquals(request.prisonCode)
    with(request) {
      prisonerNumber?.let {
        // If a prisonerNumber is supplied restrict results only to those relating to this person
        spec = spec.and(eventReviewSearchSpecification.prisonerNumberEquals(prisonerNumber))
      }
      eventDate?.let {
        // Restrict results to the time period of the date supplied (start to end of day)
        spec = spec.and(
          eventReviewSearchSpecification.eventTimeBetween(
            eventDate.atStartOfDay(),
            eventDate.plusDays(1).atStartOfDay(),
          ),
        )
      }
      acknowledgedEvents?.let {
        // If acknowledgedEvents is false exclude any with an acknowledgedTime set
        if (!it) {
          spec = spec.and(eventReviewSearchSpecification.isNotAcknowledged())
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

  @Transactional
  fun acknowledgeEvents(prisonCode: String, req: EventAcknowledgeRequest, name: String) {
    val updatedEvents = eventReviewRepository.findAllById(req.eventReviewIds)
    updatedEvents.map {
      it.acknowledgedBy = name
      it.acknowledgedTime = LocalDateTime.now()
    }
    eventReviewRepository.saveAllAndFlush(updatedEvents)

    val propertiesMap = mapOf(
      ACKNOWLEDGED_BY_KEY to name,
      ACKNOWLEDGED_TIME_KEY to LocalDateTime.now().toString(),
      EVENT_REVIEW_IDS_KEY to req.eventReviewIds.joinToString { "," },
    )
    val metricsMap = mapOf(
      NUMBER_OF_RESULTS_KEY to 1.0,
    )
    telemetryClient.trackEvent(TelemetryEvent.ACTIVITY_CREATED.value, propertiesMap, metricsMap)
  }

  private fun createSort(sortDirection: String, sortField: String = "eventTime"): Sort? {
    return when (sortDirection) {
      "ascending" -> Sort.by(sortField).ascending()
      "descending" -> Sort.by(sortField).descending()
      else -> null
    }
  }
}
