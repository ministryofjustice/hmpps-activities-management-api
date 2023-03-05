package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

@Entity
@Table(name = "activity_category")
data class ActivityCategory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityCategoryId: Long = -1,

  val code: String,

  val name: String,

  val description: String?,
) {
  fun toModel() = ModelActivityCategory(
    id = activityCategoryId,
    code = code,
    name = name,
    description = description,
  )
}

fun List<ActivityCategory>.toModel() = map { it.toModel() }
