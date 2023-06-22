package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.PersistenceException
import jakarta.persistence.TypedQuery
import org.hibernate.Session
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import java.time.LocalDate
import java.util.Optional

interface ActivityRepositoryCustom {
  fun getActivityByIdWithFilters(activityId: Long, earliestSessionDate: LocalDate): Optional<Activity>
}

class ActivityRepositoryImpl : ActivityRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Override
  override fun getActivityByIdWithFilters(activityId: Long, earliestSessionDate: LocalDate): Optional<Activity> {
    val session = entityManager.unwrap(Session::class.java)

    // Enable the session date filter to limit the scheduled instances returned
    log.info("Enabling filter SessionDateFilter with earliestSessionDate: $earliestSessionDate")
    val sessionDateFilter = session.enableFilter("SessionDateFilter")
    sessionDateFilter.setParameter("earliestSessionDate", earliestSessionDate)

    val hql = "select a from Activity a where a.activityId = :activityId"
    val query: TypedQuery<Activity> = entityManager.createQuery(hql, Activity::class.java)
    query.setParameter("activityId", activityId)

    try {
      val activity = query.singleResult
      return Optional.of(activity)
    } catch (e: PersistenceException) {
      log.error("Activity by ID failed ${e.message}")
    }

    return Optional.empty()
  }
}
