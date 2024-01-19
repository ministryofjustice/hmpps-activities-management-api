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

  fun getActivitySchedulesWithFilteredInstances(prisonCode: String, earliestSessionDate: LocalDate): List<ActivitySchedule>
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
      log.info("Enabling filter $SESSION_DATE_FILTER with earliestSessionDate: $earliestSessionDate")
      val sessionDateFilter = session.enableFilter(SESSION_DATE_FILTER)
      sessionDateFilter.setParameter("earliestSessionDate", earliestSessionDate)
    }

    if (allocationsActiveOnDate != null) {
      log.info("Enabling filter $ALLOCATION_DATE_FILTER with allocationsActiveOnDate: $allocationsActiveOnDate")
      val allocationsDateFilter = session.enableFilter(ALLOCATION_DATE_FILTER)
      allocationsDateFilter.setParameter("allocationsActiveOnDate", allocationsActiveOnDate)
    }

    return runCatching {
      query.singleResult
    }.onFailure { log.error("ActivitySchedule by ID with filters ${it.message}") }
      .getOrNull()
  }

  @Override
  override fun getActivitySchedulesWithFilteredInstances(
    prisonCode: String,
    earliestSessionDate: LocalDate,
  ): List<ActivitySchedule> {
    val session = entityManager.unwrap(Session::class.java)

    val hql = "SELECT s from ActivitySchedule s where s.activity.prisonCode = :prisonCode"
    val query: TypedQuery<ActivitySchedule> = entityManager.createQuery(hql, ActivitySchedule::class.java)
    query.setParameter("prisonCode", prisonCode)

    session
      .enableFilter(SESSION_DATE_FILTER)
      .setParameter("earliestSessionDate", earliestSessionDate)

    return runCatching {
      query.resultList.toList()
    }.onFailure { log.error("ActivitySchedule by ID with filters ${it.message}") }
      .getOrDefault(emptyList())
  }
}
