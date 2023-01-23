package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "prison_pay_band")
data class PrisonPayBand(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  val prisonPayBandId: Long = -1,

  @Column(length = 10, nullable = false)
  val prisonCode: String,

  @Column(nullable = false)
  val displaySequence: Int,

  @Column(length = 30, nullable = false)
  val payBandAlias: String,

  @Column(length = 100, nullable = false)
  val payBandDescription: String,

  @Column(name = "nomis_pay_band", nullable = false)
  val nomisPayBand: Int
)
