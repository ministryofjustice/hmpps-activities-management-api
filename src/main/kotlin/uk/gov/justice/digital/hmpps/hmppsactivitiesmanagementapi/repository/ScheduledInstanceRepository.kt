package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import java.time.LocalDate

interface ScheduledInstanceRepository : JpaRepository<ScheduledInstance, Long> {
  fun findAllBySessionDate(date: LocalDate): List<ScheduledInstance>

  // TODO - should it check for suspensions? Or done in the client? (I added the allocation date checks)

  @Query(
    """
    SELECT si FROM ScheduledInstance si 
    WHERE EXISTS (
      SELECT 1 FROM si.activitySchedule.allocations a 
      WHERE a.activitySchedule.activity.prisonCode = :prisonCode 
      AND si.sessionDate >= :startDate
      AND si.sessionDate <= :endDate
      AND (si.cancelled is null or si.cancelled = false)
      AND a.startDate <= si.sessionDate AND (a.endDate is null or a.endDate >= si.sessionDate)
    )
    """
  )
  fun getActivityScheduleInstancesByPrisonCodeAndDateRange(
    prisonCode: String,
    startDate: LocalDate,
    endDate: LocalDate
  ): List<ScheduledInstance>
}
