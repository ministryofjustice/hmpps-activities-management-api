package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPayHistory

@Repository
interface ActivityPayHistoryRepository : JpaRepository<ActivityPayHistory, Long> {

  @Query(
    value =
    """
    SELECT aph FROM ActivityPayHistory aph
    WHERE aph.activity = :activity
    ORDER BY CASE WHEN aph.changedTime IS NULL THEN 1 ELSE 0 END, aph.changedTime DESC
    """,
  )
  fun findByActivityOrderByChangedTimeDesc(activity: Activity): List<ActivityPayHistory>
}
