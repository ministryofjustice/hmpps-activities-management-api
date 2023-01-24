package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "activity_tier")
data class ActivityTier(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityTierId: Long = -1,

  @Column(nullable = false)
  val code: String,

  @Column(nullable = false)
  val description: String,
)
