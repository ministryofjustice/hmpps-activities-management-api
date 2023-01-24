package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity
@Table(name = "activity_pay")
data class ActivityPay(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityPayId: Long = -1,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  var incentiveLevel: String? = null,

  var payBand: String? = null,

  var rate: Int? = null,

  var pieceRate: Int? = null,

  var pieceRateItems: Int? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ActivityPay

    return activityPayId == other.activityPayId
  }

  override fun hashCode(): Int = activityPayId.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(activityPayId = $activityPayId )"
  }
}
