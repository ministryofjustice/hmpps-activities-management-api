package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPayHistory

@Repository
interface ActivityPayHistoryRepository : JpaRepository<ActivityPayHistory, Long> {

  fun findByActivityOrderByChangedTimeDesc(
    activity: Activity,
  ): List<ActivityPayHistory>
}
