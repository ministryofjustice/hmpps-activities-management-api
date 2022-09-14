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
  val activityPayId: Int? = null,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  val prisonCode: String,

  var iepBasicRate: Int? = null,

  var iepStandardRate: Int? = null,

  var iepEnhancedRate: Int? = null,

  var pieceRate: Int? = null,

  var pieceRateItems: Int? = null,
)
