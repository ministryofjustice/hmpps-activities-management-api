package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityBasic
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import java.time.LocalDate

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

  @Query(value = "SELECT a FROM Activity a WHERE a.prisonCode = :prisonCode AND a.summary = :summary AND a.activityId  <> :activityId")
  fun getActivityByPrisonCodeAndSummaryAndActivityId(
    @Param("prisonCode") prisonCode: String,
    @Param("summary") summary: String,
    @Param("activityId") activityId: Long,
  ): List<Activity>

  fun findByActivityIdAndPrisonCode(activityId: Long, prisonCode: String): Activity?

  @Query(value = "SELECT ab from ActivityBasic ab WHERE ab.activityId = :activityId")
  fun getActivityBasicById(@Param("activityId") activityId: Long): ActivityBasic?

  @Query(value = "SELECT ab FROM ActivityBasic ab WHERE ab.prisonCode = :prisonCode")
  fun getActivityBasicByPrisonCode(@Param("prisonCode") prisonCode: String): List<ActivityBasic>

  @Query(
    value =
    """
    SELECT ab FROM ActivityBasic ab
    WHERE ab.prisonCode = :prisonCode
    AND ab.startDate <= :toDate
    AND (ab.endDate is null or ab.endDate >= :fromDate)
    """,
  )
  fun getBasicForPrisonBetweenDates(
    @Param("prisonCode") prisonCode: String,
    @Param("fromDate") fromDate: LocalDate,
    @Param("toDate") toDate: LocalDate,
  ): List<ActivityBasic>
}
