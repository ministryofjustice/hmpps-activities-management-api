package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "event_tier")
data class EventTier(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val eventTierId: Long = 0,

  @Column(nullable = false)
  val code: String,

  @Column(nullable = false)
  val description: String,
) {
  fun isTierTwo() = EventTierType.valueOf(this.code) == EventTierType.TIER_2

  fun isFoundation() = EventTierType.valueOf(this.code) == EventTierType.FOUNDATION
}

enum class EventTierType {
  TIER_1,
  TIER_2,
  FOUNDATION,
}
