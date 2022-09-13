package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
data class Activity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityId: Int = -1,

  @ManyToOne
  @JoinColumn(name = "rollout_prison_id", nullable = false)
  val rolloutPrison: RolloutPrison,

  @ManyToOne
  @JoinColumn(name = "activity_category_id", nullable = false)
  val activityCategory: ActivityCategory,

  @ManyToOne
  @JoinColumn(name = "activity_tier", nullable = false)
  val activityTier: ActivityTier,

  var summary: String? = null,

  var description: String? = null,

  var startDate: LocalDate? = null,

  var endDate: LocalDate? = null,

  var isActive: Boolean = false,

  val createdAt: LocalDateTime,

  val createdBy: String
)
