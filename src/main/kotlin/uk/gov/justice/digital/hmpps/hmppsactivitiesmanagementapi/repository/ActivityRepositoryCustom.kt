package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.TypedQuery
import org.hibernate.Session
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import java.time.LocalDate
import java.util.Optional

interface ActivityRepositoryCustom {
  fun getActivityByIdWithFilters(
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

  @Override
  override fun getActivityByIdWithFilters(
    activityId: Long,
    earliestSessionDate: LocalDate,
    earliestAllocationEndDate: LocalDate,
  ): Optional<Activity> {
    val session = entityManager.unwrap(Session::class.java)

    // NOTE: This filter is enabled because the "instances" collection is marked as FetchType.LAZY.
    // Enable the session date filter to limit the scheduled instances returned
    log.info("Enabling filter SessionDateFilter with earliestSessionDate: $earliestSessionDate")
    val sessionDateFilter = session.enableFilter("SessionDateFilter")
    sessionDateFilter.setParameter("earliestSessionDate", earliestSessionDate)

    // NOTE: This filter is not enabled because the "allocations" collection is marked as FetchType.EAGER.
    // Enable the allocation end date filter to limit the allocations returned
    log.info("Enabling filter AllocationEndDateFilter with earliestEndDate: $earliestAllocationEndDate")
    val endDateFilter = session.enableFilter("AllocationEndDateFilter")
    endDateFilter.setParameter("earliestEndDate", earliestAllocationEndDate)

    val hql = "select a from Activity a where a.activityId = :activityId"
    val query: TypedQuery<Activity> = entityManager.createQuery(hql, Activity::class.java)
    query.setParameter("activityId", activityId)

    try {
      val activity = query.singleResult
      return Optional.of(activity)
    } catch (e: Exception) {
      log.error("Activity by ID failed ${e.message}")
    }

    return Optional.empty()
  }
}
