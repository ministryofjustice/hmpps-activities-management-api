package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

@Entity
@Table(name = "activity_category")
data class ActivityCategory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityCategoryId: Long? = null,

  val code: String,

  val description: String
) {
  fun toModel() = ModelActivityCategory(
    id = activityCategoryId!!,
    description = description
  )
}

fun List<ActivityCategory>.toModel() = map { it.toModel() }
