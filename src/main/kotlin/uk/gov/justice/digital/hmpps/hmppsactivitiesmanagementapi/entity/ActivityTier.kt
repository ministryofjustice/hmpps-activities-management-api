package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

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
