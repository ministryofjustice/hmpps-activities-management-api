package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand as ModelPrisonPayBand

@Entity
@Table(name = "prison_pay_band")
data class PrisonPayBand(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  val prisonPayBandId: Long = 0,

  @Column(length = 10, nullable = false)
  val prisonCode: String,

  @Column(nullable = false)
  var displaySequence: Int,

  @Column(length = 30, nullable = false)
  var payBandAlias: String,

  @Column(length = 100, nullable = false)
  var payBandDescription: String,

  @Column(name = "nomis_pay_band", nullable = false)
  var nomisPayBand: Int,

  val createdTime: LocalDateTime? = null,

  val createdBy: String? = null,

  var updatedTime: LocalDateTime? = null,

  var updatedBy: String? = null,
) {
  fun toModel() = ModelPrisonPayBand(

    id = prisonPayBandId,
    displaySequence = displaySequence,
    alias = payBandAlias,
    description = payBandDescription,
    nomisPayBand = nomisPayBand,
    prisonCode = prisonCode,
    createdTime = createdTime,
    createdBy = createdBy,
    updatedTime = updatedTime,
    updatedBy = updatedBy,
  )
}
