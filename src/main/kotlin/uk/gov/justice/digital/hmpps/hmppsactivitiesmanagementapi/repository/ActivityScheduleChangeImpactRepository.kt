package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleChangeImpact

@Repository
interface ActivityScheduleChangeImpactRepository : JpaRepository<ActivityScheduleChangeImpact, Long> {

  // Rows are written one-per-week (see ActivityService.applySlotsUpdate()), so DISTINCT ON (week_number) ordered
  // by changed_at desc returns just the single most recent row for each week - 1 or 2 rows regardless of how
  // much history exists for the allocation.
  @Query(
    value = """
      SELECT DISTINCT ON (week_number) *
      FROM activity_schedule_change_impact
      WHERE allocation_id = :allocationId
      ORDER BY week_number, changed_at DESC
    """,
    nativeQuery = true,
  )
  fun findLatestImpactPerWeekByAllocationId(allocationId: Long): List<ActivityScheduleChangeImpact>
}
