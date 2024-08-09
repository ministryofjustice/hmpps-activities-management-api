package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import java.time.LocalDate

interface ScheduledInstanceRepository : JpaRepository<ScheduledInstance, Long> {
  fun findAllBySessionDate(date: LocalDate): List<ScheduledInstance>

  @Query(
    """
    SELECT si FROM ScheduledInstance si 
    WHERE EXISTS (
      SELECT 1 FROM si.activitySchedule s
      WHERE s.activity.prisonCode = :prisonCode 
      AND si.sessionDate >= :startDate
      AND si.sessionDate <= :endDate
      AND (:timeSlot is null or si.time_slot = :timeSlot)
    ) AND (:cancelled is null or si.cancelled = :cancelled)
    """,
  )
  fun getActivityScheduleInstancesByPrisonCodeAndDateRange(
    prisonCode: String,
    startDate: LocalDate,
    endDate: LocalDate,
    cancelled: Boolean? = null,
    timeSlot: TimeSlot? = null,
  ): List<ScheduledInstance>
}
