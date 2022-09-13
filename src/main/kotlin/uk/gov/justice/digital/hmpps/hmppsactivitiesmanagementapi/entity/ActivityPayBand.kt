package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "activity_pay_band")
data class ActivityPayBand(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityPayBandId: Int = -1,

  @ManyToOne
  @JoinColumn(name = "activity_pay_id", nullable = false)
  val activityPay: ActivityPay,

  var payBand: String? = null,

  var rate: Int? = null,

  var pieceRate: Int? = null,

  var pieceRateItems: Int? = null,
)
