package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "activity_pay")
data class ActivityPay(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityPayId: Long? = null,

  @OneToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  @OneToMany(mappedBy = "activityPay", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  val payBands: MutableList<ActivityPayBand> = mutableListOf(),

  var iepBasicRate: Int? = null,

  var iepStandardRate: Int? = null,

  var iepEnhancedRate: Int? = null,

  var pieceRate: Int? = null,

  var pieceRateItems: Int? = null,
)
