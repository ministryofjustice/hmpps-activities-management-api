package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPay

@Repository
interface ActivityPayRepository : JpaRepository<ActivityPay, Long> {

  @Query(
    value =
    """
    SELECT distinct ap.activity.activityId FROM ActivityPay ap
    ORDER BY ap.activity.activityId ASC
    """,
  )
  fun getDistinctActivityId(): List<Long>

  @Query(
    value =
    """
    SELECT ap FROM ActivityPay ap
    WHERE ap.activity.activityId = :activityId
    ORDER BY ap.incentiveNomisCode, ap.payBand.prisonPayBandId, CASE WHEN ap.startDate IS NULL THEN 0 ELSE 1 END, ap.startDate ASC
    """,
  )
  fun findByActivityId(activityId: Long): List<ActivityPay>
}
