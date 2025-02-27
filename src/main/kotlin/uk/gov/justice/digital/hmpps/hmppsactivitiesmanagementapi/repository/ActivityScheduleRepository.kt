package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import java.util.UUID

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
}
