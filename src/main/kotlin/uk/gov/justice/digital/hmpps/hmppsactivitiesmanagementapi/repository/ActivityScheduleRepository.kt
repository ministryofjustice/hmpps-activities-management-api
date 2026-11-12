package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import java.util.*

@Repository
interface ActivityScheduleRepository :
  JpaRepository<ActivitySchedule, Long>,
  ActivityScheduleRepositoryCustom {
  fun findAllByActivityPrisonCode(prisonCode: String): List<ActivitySchedule>

  @EntityGraph(attributePaths = ["instances"], type = EntityGraph.EntityGraphType.LOAD)
  fun getAllByActivity(activity: Activity): List<ActivitySchedule>

  @Query(
    value = """
    from ActivitySchedule s where s.activityScheduleId = :id and s.activity.prisonCode = :prisonCode
  """,
  )
  fun findBy(@Param("id") id: Long, @Param("prisonCode") prisonCode: String): ActivitySchedule?

  @Query(
    """
    select distinct a.internalLocationId 
    from ActivitySchedule a 
    where a.internalLocationId is not null
    and a.dpsLocationId is null
  """,
  )
  fun findNomisLocationsIds(): List<Int>

  @Query(
    value = """
    update ActivitySchedule a 
    set a.dpsLocationId = :dpsLocationId, 
    a.internalLocationCode = :code, 
    a.internalLocationDescription = :description 
    where a.internalLocationId = :internalLocationId
  """,
  )
  @Modifying
  fun updateLocationDetails(internalLocationId: Int, dpsLocationId: UUID, code: String, description: String)

  @Query(
    value = """
      select distinct a.dpsLocationId from ActivitySchedule a
      where a.activity.prisonCode = :prisonCode
      and (a.activity.endDate is null or a.activity.endDate >= current_date)
      and a.dpsLocationId is not null
    """,
  )
  fun findInvalidLocationUuids(prisonCode: String): List<UUID>

  @Query(
    value = """
      select 
        a.activity.prisonCode as prisonCode,
        a.activity.activityId as activityId,
        a.activity.description as activityDescription,
        a.internalLocationId as internalLocationId,
        a.internalLocationCode as internalLocationCode,
        a.internalLocationDescription as internalLocationDescription,
        a.dpsLocationId as dpsLocationId
      from ActivitySchedule a 
      where a.dpsLocationId in :dpsLocationId
      and (a.activity.endDate is null or a.activity.endDate >= current_date)
      and a.activity.onWing = false
      and a.activity.offWing = false
      and a.activity.inCell = false
      order by a.activity.description
    """,
  )
  fun findByInvalidLocationUuids(dpsLocationId: UUID): List<ActivityScheduleWithInvalidLocation>

  interface ActivityScheduleWithInvalidLocation {
    fun getPrisonCode(): String
    fun getActivityId(): Long
    fun getActivityDescription(): String
    fun getInternalLocationId(): Int
    fun getInternalLocationCode(): String
    fun getInternalLocationDescription(): String
    fun getDpsLocationId(): UUID
  }
}
