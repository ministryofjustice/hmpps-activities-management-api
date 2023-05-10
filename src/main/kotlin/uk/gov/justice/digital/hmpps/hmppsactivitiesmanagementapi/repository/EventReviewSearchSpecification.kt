package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReview
import java.time.LocalDateTime

@Component
class EventReviewSearchSpecification {
  fun prisonCodeEquals(prisonCode: String) =
    Specification<EventReview> { root, _, cb -> cb.equal(root.get<String>("prisonCode"), prisonCode) }

  fun prisonerNumberEquals(prisonerNumber: String) =
    Specification<EventReview> { root, _, cb -> cb.equal(root.get<String>("prisonerNumber"), prisonerNumber) }

  fun eventTimeBetween(startTime: LocalDateTime, endTime: LocalDateTime) =
    Specification<EventReview> { root, _, cb -> cb.between(root.get("eventTime"), startTime, endTime) }

  fun isNotAcknowledged() =
    Specification<EventReview> { root, _, cb -> cb.isNull(root.get<LocalDateTime>("acknowledgedTime")) }

  fun isAcknowledged() =
    Specification<EventReview> { root, _, cb -> cb.isNotNull(root.get<LocalDateTime>("acknowledgedTime")) }
}
