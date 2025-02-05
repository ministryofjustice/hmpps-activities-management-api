package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import java.time.LocalDate

@Component
class WaitingListSearchSpecification {
  fun prisonCodeEquals(prisonCode: String) = Specification<WaitingList> { root, _, cb -> cb.equal(root.get<String>("prisonCode"), prisonCode) }

  fun applicationDateFrom(requestDateFrom: LocalDate): Specification<WaitingList> = Specification<WaitingList> { root, _, cb -> cb.greaterThanOrEqualTo(root.get("applicationDate"), requestDateFrom) }

  fun applicationDateTo(requestDateTo: LocalDate): Specification<WaitingList> = Specification<WaitingList> { root, _, cb -> cb.lessThanOrEqualTo(root.get("applicationDate"), requestDateTo) }

  fun activityIdEqual(activityId: Long) = Specification<WaitingList> { root, _, cb ->
    cb.equal(
      root.join<WaitingList, Activity>("activity").get<Long>("activityId"),
      activityId,
    )
  }

  fun prisonerNumberIn(prisonerNumbers: List<String>) = Specification<WaitingList> { root, _, _ -> root.get<String>("prisonerNumber").`in`(prisonerNumbers) }

  fun statusIn(status: List<WaitingListStatus>) = Specification<WaitingList> { root, _, _ -> root.get<WaitingListStatus>("status").`in`(status) }
}
