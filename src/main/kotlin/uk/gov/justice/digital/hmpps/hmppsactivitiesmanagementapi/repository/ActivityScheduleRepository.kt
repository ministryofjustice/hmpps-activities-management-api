package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule

@Repository
interface ActivityScheduleRepository : JpaRepository<ActivitySchedule, Long>, ActivityScheduleRepositoryCustom {
  @EntityGraph(attributePaths = ["instances"], type = EntityGraph.EntityGraphType.LOAD)
  fun findAllByActivity_PrisonCode(prisonCode: String): List<ActivitySchedule>

  @EntityGraph(attributePaths = ["instances"], type = EntityGraph.EntityGraphType.LOAD)
  fun getAllByActivity(activity: Activity): List<ActivitySchedule>
}
