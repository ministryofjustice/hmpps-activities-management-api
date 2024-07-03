package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

enum class ActivityCategoryCode {
  SAA_EDUCATION,
  SAA_INDUSTRIES,
  SAA_PRISON_JOBS,
  SAA_GYM_SPORTS_FITNESS,
  SAA_INDUCTION,
  SAA_INTERVENTIONS,
  SAA_FAITH_SPIRITUALITY,
  SAA_NOT_IN_WORK,
  SAA_OTHER,
}

@Entity
@Table(name = "activity_category")
data class ActivityCategory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityCategoryId: Long = 0,

  val code: String,

  val name: String,

  val description: String?,
) {

  fun isNotInWork() = code == "SAA_NOT_IN_WORK"

  fun toModel() = ModelActivityCategory(
    id = activityCategoryId,
    code = code,
    name = name,
    description = description,
  )
}

fun List<ActivityCategory>.toModel() = map { it.toModel() }
