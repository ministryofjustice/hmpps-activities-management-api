package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata

import jakarta.validation.ValidationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import java.time.LocalDate

data class PrisonPlan(
  val code: String,
  val activities: Boolean,
  val appointments: Boolean,
  val externalActivities: Boolean,
  val prisonLive: Boolean,
)

@Service
class RolloutPrisonService(
  @Value("\${migrate.activities-live}") private val activitiesLive: String,
  @Value("\${migrate.appointments-live}") private val appointmentsLive: String,
  @Value("\${migrate.external-activities-live}") private val externalActivitiesLive: String,
  @Value("\${migrate.prisons-live}") private val prisonsLive: String,
) {

  private fun getPrison(code: String): PrisonPlan {
    val activities = activitiesLive.split(",").contains(code)
    val appointments = appointmentsLive.split(",").contains(code)
    val externalActivities = externalActivitiesLive.split(",").contains(code)
    val prisonLive = prisonsLive.split(",").contains(code)

    return PrisonPlan(
      code = code,
      activities = activities,
      appointments = appointments,
      externalActivities = externalActivities,
      prisonLive = prisonLive,
    )
  }

  fun getByPrisonCode(code: String): RolloutPrisonPlan {
    val prisonPlan = getPrison(code = code)

    return RolloutPrisonPlan(
      prisonCode = code,
      activitiesRolledOut = prisonPlan.activities,
      appointmentsRolledOut = prisonPlan.appointments,
      externalActivitiesRolledOut = prisonPlan.externalActivities,
      prisonLive = prisonPlan.prisonLive,
    )
  }

  fun isAppointmentsRolledOut(code: String) {
    if (appointmentsLive.split(",").contains(code).not()) throw ValidationException("prison not active")
  }

  fun isActivitiesRolledOutAt(prisonCode: String): Boolean = getPrison(code = prisonCode).activities

  fun getRolloutPrisons(prisonsLive: Boolean = false): List<RolloutPrisonPlan> = if (prisonsLive) {
    this.prisonsLive.split(",").map { getByPrisonCode(it) }
  } else {
    activitiesLive.split(",").map { getByPrisonCode(it) }
  }

  companion object {
    fun RolloutPrisonPlan.hasExpired(predicate: () -> LocalDate?) = predicate()?.onOrBefore(LocalDate.now().minusDays(this.maxDaysToExpiry.toLong())) == true
  }
}
