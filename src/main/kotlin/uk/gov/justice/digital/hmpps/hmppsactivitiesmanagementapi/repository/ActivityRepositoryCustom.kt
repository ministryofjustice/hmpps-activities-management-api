package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.TypedQuery
import org.hibernate.Session
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SESSION_DATE_FILTER
import java.time.LocalDate

interface ActivityRepositoryCustom {
  fun getActivityByIdWithFilters(activityId: Long, earliestSessionDate: LocalDate): Activity?

  fun findByActivityIdAndPrisonCodeWithFilters(
    activityId: Long,
    prisonCode: String,
    earliestSessionDate: LocalDate,
  ): Activity?
}

class ActivityRepositoryImpl : ActivityRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Override
  override fun getActivityByIdWithFilters(activityId: Long, earliestSessionDate: LocalDate): Activity? {
    val session = entityManager.unwrap(Session::class.java)

    log.info("Enabling filter SessionDateFilter with earliestSessionDate: $earliestSessionDate")
    val sessionDateFilter = session.enableFilter(SESSION_DATE_FILTER)
    sessionDateFilter.setParameter("earliestSessionDate", earliestSessionDate)

    val hql = "select a from Activity a where a.activityId = :activityId"
    val query: TypedQuery<Activity> = entityManager.createQuery(hql, Activity::class.java)
    query.setParameter("activityId", activityId)

    return runCatching {
      query.singleResult
    }.onFailure { log.error("Activity by ID with filters ${it.message}") }
      .getOrNull()
  }

  @Override
  override fun findByActivityIdAndPrisonCodeWithFilters(
    activityId: Long,
    prisonCode: String,
    earliestSessionDate: LocalDate,
  ): Activity? {
    val session = entityManager.unwrap(Session::class.java)

    log.info("Enabling filter SessionDateFilter with earliestSessionDate: $earliestSessionDate")
    val sessionDateFilter = session.enableFilter(SESSION_DATE_FILTER)
    sessionDateFilter.setParameter("earliestSessionDate", earliestSessionDate)

    val hql = "select a from Activity a where a.activityId = :activityId and a.prisonCode = :prisonCode"
    val query: TypedQuery<Activity> = entityManager.createQuery(hql, Activity::class.java)
    query.setParameter("activityId", activityId)
    query.setParameter("prisonCode", prisonCode)

    return runCatching {
      query.singleResult
    }.onFailure { log.error("Activity by ID and prison code with filters ${it.message}") }
      .getOrNull()
  }
}
