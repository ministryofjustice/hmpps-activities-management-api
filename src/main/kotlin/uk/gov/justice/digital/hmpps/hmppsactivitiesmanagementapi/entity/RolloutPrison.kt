package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
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
    this.activitiesToBeRolledOut &&
      (
        activitiesRolloutDate?.isBefore(LocalDate.now())?.or(false) == true ||
          activitiesRolloutDate?.isEqual(LocalDate.now())?.or(false) == true
        )

  fun isAppointmentsRolledOut() =
    this.appointmentsToBeRolledOut &&
      (
        appointmentsRolloutDate?.isBefore(LocalDate.now())?.or(false) == true ||
          appointmentsRolloutDate?.isEqual(LocalDate.now())?.or(false) == true
        )
}
