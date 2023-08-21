package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySummary
import org.springframework.stereotype.Repository as RepositoryAnnotation

@RepositoryAnnotation
interface ActivitySummaryRepository : ReadOnlyRepository<ActivitySummary, Long> {
  fun findAllByPrisonCode(prisonCode: String): List<ActivitySummary>
}
