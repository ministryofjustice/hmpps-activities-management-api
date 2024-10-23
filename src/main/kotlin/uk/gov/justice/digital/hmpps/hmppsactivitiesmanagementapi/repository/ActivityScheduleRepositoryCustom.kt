package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.TypedQuery
import org.hibernate.Session
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ALLOCATION_DATE_FILTER
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SESSION_DATE_FILTER
import java.time.LocalDate

interface ActivityScheduleRepositoryCustom {
  fun getActivityScheduleByIdWithFilters(
    activityScheduleId: Long,
    earliestSessionDate: LocalDate? = null,
    allocationsActiveOnDate: LocalDate? = null,
  ): ActivitySchedule?
}

class ActivityScheduleRepositoryCustomImpl : ActivityScheduleRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Override
  override fun getActivityScheduleByIdWithFilters(
    activityScheduleId: Long,
    earliestSessionDate: LocalDate?,
    allocationsActiveOnDate: LocalDate?,
  ): ActivitySchedule? {
    val session = entityManager.unwrap(Session::class.java)

    val hql = "SELECT s from ActivitySchedule s where s.activityScheduleId = :activityScheduleId"
    val query: TypedQuery<ActivitySchedule> = entityManager.createQuery(hql, ActivitySchedule::class.java)
    query.setParameter("activityScheduleId", activityScheduleId)

    if (earliestSessionDate != null) {
      val sessionDateFilter = session.enableFilter(SESSION_DATE_FILTER)
      sessionDateFilter.setParameter("earliestSessionDate", earliestSessionDate)
    }

    if (allocationsActiveOnDate != null) {
      val allocationsDateFilter = session.enableFilter(ALLOCATION_DATE_FILTER)
      allocationsDateFilter.setParameter("allocationsActiveOnDate", allocationsActiveOnDate)
    }

    return runCatching {
      query.singleResult
    }.onFailure { log.error("ActivitySchedule by ID with filters ${it.message}") }
      .getOrNull()
  }
}
