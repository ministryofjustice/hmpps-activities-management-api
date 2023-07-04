package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate

@Entity
@Immutable
@Table(name = "v_activity_basic")
data class ActivityBasic(
  val prisonCode: String,

  @Id
  val activityId: Long = 0,

  val activityScheduleId: Long = 0,

  val summary: String?,

  val startDate: LocalDate,

  val endDate: LocalDate?,

  val categoryId: Long = 0,

  val categoryCode: String,

  val categoryName: String,
)
