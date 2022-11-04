package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.hibernate.Hibernate
import org.springframework.data.jpa.domain.AbstractPersistable_.id
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
  val activityPayId: Long? = null,

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

    return activityPayId != null && activityPayId == other.activityPayId
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(activityPayId = $activityPayId )"
  }
}
