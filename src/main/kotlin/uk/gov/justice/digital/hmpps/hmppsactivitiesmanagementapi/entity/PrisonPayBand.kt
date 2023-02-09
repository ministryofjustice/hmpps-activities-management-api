package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand as ModelPrisonPayBand

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
) {
  fun toModel() = ModelPrisonPayBand(

    id = prisonPayBandId,
    displaySequence = displaySequence,
    alias = payBandAlias,
    description = payBandDescription,
    nomisPayBand = nomisPayBand,
    prisonCode = prisonCode
  )
}
