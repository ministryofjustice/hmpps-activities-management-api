package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "activity_instance")
data class ActivityInstance(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityInstanceId: Int = -1,

  @ManyToOne
  @JoinColumn(name = "rollout_prison_id", nullable = false)
  val rolloutPrison: RolloutPrison,

  val sessionDate: LocalDate,

  val startTime: LocalDateTime,

  val endTime: LocalDateTime,

  var internalLocationId: Int? = null,

  var isCancelled: Boolean = false,

  var cancelledAt: LocalDateTime? = null,

  var cancelledBy: String? = null,
)
