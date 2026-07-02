package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.history.RevisionRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Exclusion
import java.time.DayOfWeek
import java.time.LocalDateTime

interface ExclusionHistoryAuditRow {
  val weekNumber: Int
  val timeSlot: TimeSlot
  val dayOfWeek: DayOfWeek
  val revision: Long
  val exclusionRevisionType: Int
  val exclusionDaysOfWeekRevisionType: Int
  val username: String
  val revisionDateTime: LocalDateTime?
}

@Repository
interface ExclusionRepository :
  ReadOnlyRepository<Exclusion, Long>,
  RevisionRepository<Exclusion, Long, Int> {

  @Query(
    value = """
      select
        ea.allocation_id,
        ea.week_number,
        ea.time_slot,
        eaw.day_of_week,
        ea.rev as revision,
        ea.revtype as exclusion_revision_type,
        eaw.revtype as exclusion_days_of_week_revision_type,
        eaw.id as exclusion_days_of_week_id,
        r.username,
        r.revision_date_time
        
      from exclusion_aud ea
      join exclusion_days_of_week_aud eaw on ea.rev=eaw.rev and ea.exclusion_id=eaw.exclusion_id
      join revision r on r.id = ea.rev
      where ea.allocation_id = :allocationId
    """,
    nativeQuery = true,
  )
  fun findHistoryByAllocationId(
    @Param("allocationId") allocationId: Long,
  ): List<ExclusionHistoryAuditRow>
}
