package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import java.time.LocalDate

@Entity
@Table(name = "rollout_prison")
data class RolloutPrison(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val rolloutPrisonId: Long = 0,

  val code: String,

  val description: String,

  val activitiesToBeRolledOut: Boolean,

  val activitiesRolloutDate: LocalDate? = null,

  val appointmentsToBeRolledOut: Boolean,

  val appointmentsRolloutDate: LocalDate? = null,

  val maxDaysToExpiry: Int,

) {
  fun isActivitiesRolledOut() =
    this.activitiesToBeRolledOut && activitiesRolloutDate?.onOrBefore(LocalDate.now()) == true

  fun isAppointmentsRolledOut() =
    this.appointmentsToBeRolledOut && appointmentsRolloutDate?.onOrBefore(LocalDate.now()) == true

  fun hasExpired(allocation: Allocation) =
    allocation.status(PrisonerStatus.AUTO_SUSPENDED) && hasExpired { allocation.suspendedTime?.toLocalDate() }

  fun hasExpired(predicate: () -> LocalDate?) =
    predicate()?.onOrBefore(LocalDate.now().minusDays(maxDaysToExpiry.toLong())) == true
}
