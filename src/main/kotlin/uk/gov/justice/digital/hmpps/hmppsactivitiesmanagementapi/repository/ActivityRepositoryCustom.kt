package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.TypedQuery
import org.hibernate.Session
import org.slf4j.LoggerFactory
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import java.time.LocalDate
import java.util.Optional

interface ActivityRepositoryCustom {
  fun findByIdOptimisedOld(
    activityId: Long,
    earliestSessionDate: LocalDate,
    earliestAllocationEndDate: LocalDate,
  ): Optional<Activity>
}

class ActivityRepositoryImpl : ActivityRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun findByIdOptimisedOld(
    @Param("activityId") activityId: Long,
    @Param("earliestSessionDate") earliestSessionDate: LocalDate,
    @Param("earliestAllocationEndDate") earliestAllocationEndDate: LocalDate,
  ): Optional<Activity> {
    val session = entityManager.unwrap(Session::class.java)

    log.info("FindByIdOptimised: Using parameters $activityId EarliestSessionDate: $earliestSessionDate EarliestAllocationEndDate $earliestAllocationEndDate")

    val query: TypedQuery<Activity> = session.createQuery(
      """
      select a from Activity a 
      LEFT JOIN a.schedules sch on sch.activity.activityId = a.activityId 
      LEFT JOIN sch.allocations alloc on alloc.activitySchedule.activityScheduleId = sch.activityScheduleId
      LEFT JOIN sch.instances inst on inst.activitySchedule.activityScheduleId = sch.activityScheduleId
      where a.activityId = :activityId
      and (alloc.prisonerStatus <> 'ENDED' or 
          (alloc.prisonerStatus = 'ENDED' and alloc.endDate >= :earliestAllocationEndDate))
      and inst.sessionDate >= :earliestSessionDate    
      """,
      Activity::class.java,
    )

    query
      .setParameter("activityId", activityId)
      .setParameter("earliestSessionDate", earliestSessionDate)
      .setParameter("earliestAllocationEndDate", earliestAllocationEndDate)

    try {
      val activity = query.singleResult
      return Optional.of(activity)
    } catch (e: Exception) {
      log.error("Activity by ID failed ${e.message}")
    } finally {
      session.close()
    }

    return Optional.empty()
  }
}
