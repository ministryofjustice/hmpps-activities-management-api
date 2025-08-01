package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.history.RevisionRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityBasic
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier
import java.time.LocalDate

@Repository
interface ActivityRepository :
  JpaRepository<Activity, Long>,
  RevisionRepository<Activity, Long, Long>,
  ActivityRepositoryCustom {

  fun findByPrisonCodeAndStartDateLessThan(
    @Param("prisonCode") prisonCode: String,
    @Param("startDate") startDate: LocalDate,
  ): List<Activity>

  fun findByActivityIdAndPrisonCode(activityId: Long, prisonCode: String): Activity?

  @Query(
    value =
    "select case when count(a) > 0 then true else false end " +
      "from Activity a " + "where a.prisonCode = :prisonCode " +
      "and a.summary = :summary " +
      "and (a.endDate is null or a.endDate > :endDate)",
  )
  fun existingLiveActivity(prisonCode: String, summary: String, endDate: LocalDate): Boolean

  fun findByPrisonCodeAndActivityTierAndActivityCategory(prisonCode: String, activityTier: EventTier, activityCategory: ActivityCategory): List<Activity>

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
