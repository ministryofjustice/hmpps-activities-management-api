package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel as ModelActivityMinimumEducationLevel

@Entity
@Table(name = "activity_minimum_education_level")
data class ActivityMinimumEducationLevel(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityMinimumEducationLevelId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  val educationLevelCode: String,

  val educationLevelDescription: String,

  val studyAreaCode: String,

  val studyAreaDescription: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ActivityMinimumEducationLevel

    return activityMinimumEducationLevelId == other.activityMinimumEducationLevelId
  }

  override fun hashCode(): Int = activityMinimumEducationLevelId.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(activityMinimumEducatinLevelId = $activityMinimumEducationLevelId )"

  fun toModel() = ModelActivityMinimumEducationLevel(
    id = activityMinimumEducationLevelId,
    educationLevelCode = educationLevelCode,
    educationLevelDescription = educationLevelDescription,
    studyAreaCode = studyAreaCode,
    studyAreaDescription = studyAreaDescription,
  )
}

fun List<ActivityMinimumEducationLevel>.toModel() = map { it.toModel() }
