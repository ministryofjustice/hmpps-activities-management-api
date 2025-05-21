package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPayLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonPayBand
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay

@Entity
@Audited
@Table(name = "activity_pay")
data class ActivityPay(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityPayId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  val incentiveNomisCode: String,

  var incentiveLevel: String,

  @OneToOne
  @JoinColumn(name = "prison_pay_band_id")
  var payBand: PrisonPayBand,

  var rate: Int? = null,

  var pieceRate: Int? = null,

  var pieceRateItems: Int? = null,

  val startDate: LocalDate? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ActivityPay

    return activityPayId == other.activityPayId
  }

  override fun hashCode(): Int = activityPayId.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(activityPayId = $activityPayId )"

  fun toModel() = ModelActivityPay(
    id = activityPayId,
    incentiveNomisCode = incentiveNomisCode,
    incentiveLevel = incentiveLevel,
    prisonPayBand = payBand.toModelPrisonPayBand(),
    rate = rate,
    pieceRate = pieceRate,
    pieceRateItems = pieceRateItems,
    startDate = startDate,
  )

  fun toModelLite() = ActivityPayLite(
    id = activityPayId,
    incentiveNomisCode = incentiveNomisCode,
    incentiveLevel = incentiveLevel,
    prisonPayBandId = payBand.prisonPayBandId,
    rate = rate,
    pieceRate = pieceRate,
    pieceRateItems = pieceRateItems,
  )
}
