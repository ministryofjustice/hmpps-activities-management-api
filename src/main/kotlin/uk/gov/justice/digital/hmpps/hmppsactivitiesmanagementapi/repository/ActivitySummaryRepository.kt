package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySummary

@Repository
interface ActivitySummaryRepository : ReadOnlyRepository<ActivitySummary, Long> {
  fun findAllByPrisonCode(prisonCode: String): List<ActivitySummary>
}
