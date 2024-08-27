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
)

@Service
class RolloutPrisonService(
  @Value("\${migrate.activities-live}") private val activitiesLive: String,
  @Value("\${migrate.appointments-live}") private val appointmentsLive: String,
) {

  private fun getPrison(code: String): PrisonPlan {
    val activities = activitiesLive.split(",").contains(code)
    val appointments = appointmentsLive.split(",").contains(code)

    return PrisonPlan(
      code = code,
      activities = activities,
      appointments = appointments,
    )
  }

  fun getByPrisonCode(code: String): RolloutPrisonPlan {
    val prisonPlan = getPrison(code = code)

    return RolloutPrisonPlan(
      prisonCode = code,
      activitiesRolledOut = prisonPlan.activities,
      appointmentsRolledOut = prisonPlan.appointments,
    )
  }

  fun isActive(code: String) {
    if (activitiesLive.split(",").contains(code).not()) throw ValidationException("prison not active")
  }

  fun isActivitiesRolledOutAt(prisonCode: String): Boolean = getPrison(code = prisonCode).activities

  fun getRolloutPrisons(): List<RolloutPrisonPlan> =
    activitiesLive.split(",").map { getByPrisonCode(it) }

  companion object {
    fun RolloutPrisonPlan.hasExpired(predicate: () -> LocalDate?) =
      predicate()?.onOrBefore(LocalDate.now().minusDays(this.maxDaysToExpiry.toLong())) == true
  }
}
