package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivitySummary as Model

@Entity
@Immutable
@Table(name = "v_activity_summary")
data class ActivitySummary(
  @Id
  val id: Long,
  val prisonCode: String,
  val activityName: String,
  val capacity: Int,
  val allocated: Int,
  val waitlisted: Int,
  val createdTime: LocalDateTime,

  @Enumerated(EnumType.STRING)
  val activityState: ActivityState,

  @OneToOne
  @JoinColumn(name = "activity_category_id", nullable = false)
  val activityCategory: ActivityCategory,
) {
  fun toModel() = Model(
    id = id,
    activityName = activityName,
    category = activityCategory.toModel(),
    capacity = capacity,
    allocated = allocated,
    waitlisted = waitlisted,
    createdTime = createdTime,
    activityState = activityState,
  )
}

fun List<ActivitySummary>.toModel() = map { it.toModel() }
