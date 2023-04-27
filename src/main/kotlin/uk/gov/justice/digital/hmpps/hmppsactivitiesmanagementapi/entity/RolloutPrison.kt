package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
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
) {
  fun isActivitiesRolledOut() =
    this.activitiesToBeRolledOut && activitiesRolloutDate?.onOrBefore(LocalDate.now()) == true

  fun isAppointmentsRolledOut() =
    this.appointmentsToBeRolledOut && appointmentsRolloutDate?.onOrBefore(LocalDate.now()) == true
}
