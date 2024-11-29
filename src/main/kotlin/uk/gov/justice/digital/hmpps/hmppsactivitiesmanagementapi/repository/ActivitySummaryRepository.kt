package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySummary

@Repository
interface ActivitySummaryRepository : ReadOnlyRepository<ActivitySummary, Long> {
  @Query("from ActivitySummary s join fetch s.activityCategory where s.prisonCode = :prisonCode")
  fun findAllByPrisonCode(prisonCode: String): List<ActivitySummary>
}
