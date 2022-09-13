package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "activity_pay")
data class ActivityPay(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityPayId: Int = -1,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  @ManyToOne
  @JoinColumn(name = "rollout_prison_id", nullable = false)
  val rolloutPrison: RolloutPrison,

  var iepBasicRate: Int? = null,

  var iepStandardRate: Int? = null,

  var iepEnhancedRate: Int? = null,

  var pieceRate: Int? = null,

  var pieceRateItems: Int? = null,
)
