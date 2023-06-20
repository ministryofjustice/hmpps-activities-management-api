package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import java.time.LocalDate
import java.util.Optional

interface ScheduledInstanceRepository : JpaRepository<ScheduledInstance, Long> {
  @EntityGraph(attributePaths = ["activitySchedule.allocations", "activitySchedule.activity"], type = EntityGraph.EntityGraphType.LOAD)
  fun findAllBySessionDate(date: LocalDate): List<ScheduledInstance>

  @Query(
    """
    SELECT si FROM ScheduledInstance si 
    WHERE EXISTS (
      SELECT 1 FROM si.activitySchedule s
      WHERE s.activity.prisonCode = :prisonCode 
      AND si.sessionDate >= :startDate
      AND si.sessionDate <= :endDate
    )
    """,
  )
  @EntityGraph(attributePaths = ["attendances"], type = EntityGraph.EntityGraphType.LOAD)
  fun getActivityScheduleInstancesByPrisonCodeAndDateRange(
    prisonCode: String,
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<ScheduledInstance>

  @Query(
    """
    FROM ScheduledInstance si WHERE si.scheduledInstanceId = :id
    """,
  )
  @EntityGraph(attributePaths = ["attendances"], type = EntityGraph.EntityGraphType.LOAD)
  fun findByIdQuery(id: Long): Optional<ScheduledInstance>
}
