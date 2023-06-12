package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import java.time.LocalDate
import java.util.Optional

@Repository
interface ActivityRepository : JpaRepository<Activity, Long>, ActivityRepositoryCustom {
  @Query(value = "from Activity a where a.prisonCode = :prison and a.startDate <= :date and (a.endDate is null or a.endDate >= :date)")
  fun getAllForPrisonAndDate(@Param("prison") prison: String, @Param("date") date: LocalDate): List<Activity>

  @Query(
    value =
    """
    from Activity a
    where a.prisonCode = :prisonCode
    and a.startDate <= :toDate
    and (a.endDate is null or a.endDate >= :fromDate)
    """,
  )
  fun getAllForPrisonBetweenDates(
    @Param("prisonCode") prisonCode: String,
    @Param("fromDate") fromDate: LocalDate,
    @Param("toDate") toDate: LocalDate,
  ): List<Activity>

  fun getAllByPrisonCodeAndActivityCategory(prisonCode: String, category: ActivityCategory): List<Activity>
  fun getAllByPrisonCode(prisonCode: String): List<Activity>
  fun existsActivityByPrisonCodeAndSummary(prisonCode: String, summary: String): Boolean

  @Query(
    """
    from Activity a  
    LEFT JOIN a.schedules sch on sch.activity.activityId = a.activityId 
    LEFT JOIN sch.allocations alloc on alloc.activitySchedule.activityScheduleId = sch.activityScheduleId
    LEFT JOIN sch.instances inst on inst.activitySchedule.activityScheduleId = sch.activityScheduleId
    where a.activityId = :activityId
    and (alloc.prisonerStatus <> 'ENDED' or 
        (alloc.prisonerStatus = 'ENDED' and alloc.endDate >= :earliestAllocationEndDate))
    and inst.sessionDate >= :earliestSessionDate 
    """,
  )
  fun findByIdOptimised(
    @Param("activityId") activityId: Long,
    @Param("earliestSessionDate") earliestSessionDate: LocalDate,
    @Param("earliestAllocationEndDate") earliestAllocationEndDate: LocalDate,
  ): Optional<Activity>
}
