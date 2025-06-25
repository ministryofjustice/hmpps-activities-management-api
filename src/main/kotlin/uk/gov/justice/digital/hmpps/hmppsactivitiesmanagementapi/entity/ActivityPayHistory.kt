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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonPayBand
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPayHistory as ModelActivityPayHistory

@Entity
@Table(name = "activity_pay_history")
data class ActivityPayHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityPayHistoryId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  val incentiveNomisCode: String?,

  var incentiveLevel: String?,

  @OneToOne
  @JoinColumn(name = "prison_pay_band_id")
  var payBand: PrisonPayBand,

  var rate: Int? = null,

  val startDate: LocalDate? = null,

  val changedDetails: String?,

  val changedTime: LocalDateTime?,

  val changedBy: String?,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ActivityPayHistory

    return activityPayHistoryId == other.activityPayHistoryId
  }

  override fun hashCode(): Int = activityPayHistoryId.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(activityPayHistoryId = $activityPayHistoryId )"

  fun toModel() = ModelActivityPayHistory(
    id = activityPayHistoryId,
    incentiveNomisCode = incentiveNomisCode,
    incentiveLevel = incentiveLevel,
    prisonPayBand = payBand.toModelPrisonPayBand(),
    rate = rate,
    startDate = startDate,
    changedDetails = changedDetails,
    changedTime = changedTime,
    changedBy = changedBy,
  )
}
