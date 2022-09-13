package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "activity_category")
data class ActivityCategory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityCategoryId: Int = -1,

  val categoryCode: String,

  val description: String
)
