package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.PersistenceException
import jakarta.persistence.TypedQuery
import org.hibernate.Session
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import java.time.LocalDate
import java.util.Optional

interface ActivityScheduleRepositoryCustom {
  fun getActivityScheduleByIdWithFilters(activityScheduleId: Long, earliestSessionDate: LocalDate): Optional<ActivitySchedule>
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
    earliestSessionDate: LocalDate,
  ): Optional<ActivitySchedule> {
    val session = entityManager.unwrap(Session::class.java)

    // Enable the session date filter to limit the scheduled instances returned
    log.info("Enabling filter SessionDateFilter with earliestSessionDate: $earliestSessionDate")
    val sessionDateFilter = session.enableFilter("SessionDateFilter")
    sessionDateFilter.setParameter("earliestSessionDate", earliestSessionDate)

    val hql = "SELECT s from ActivitySchedule s where s.activityScheduleId = :activityScheduleId"
    val query: TypedQuery<ActivitySchedule> = entityManager.createQuery(hql, ActivitySchedule::class.java)
    query.setParameter("activityScheduleId", activityScheduleId)

    try {
      val activitySchedule = query.singleResult
      return Optional.of(activitySchedule)
    } catch (e: PersistenceException) {
      log.error("ActivitySchedule by ID failed ${e.message}")
    }

    return Optional.empty()
  }
}
