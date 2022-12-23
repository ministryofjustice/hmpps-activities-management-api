package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.LocalDate
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "rollout_prison")
data class RolloutPrison(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val rolloutPrisonId: Long? = null,

  val code: String,

  val description: String,

  var active: Boolean = false,

  val rolloutDate: LocalDate,
)
