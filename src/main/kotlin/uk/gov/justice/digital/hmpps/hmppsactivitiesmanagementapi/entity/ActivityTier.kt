package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "activity_tier")
data class ActivityTier(
  @Id
  val activityTier: Int,

  @Column(nullable = false)
  val description: String
)
