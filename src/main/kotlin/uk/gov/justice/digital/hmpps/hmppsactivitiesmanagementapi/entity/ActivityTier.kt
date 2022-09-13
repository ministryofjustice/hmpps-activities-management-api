package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "activity_tier")
class ActivityTier(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityTier: Int = -1,

  val description: String
)
