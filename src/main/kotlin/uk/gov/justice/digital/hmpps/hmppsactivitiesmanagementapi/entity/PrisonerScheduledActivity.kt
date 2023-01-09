package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.hibernate.annotations.Immutable
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.Table

/*
 * Read-only entity for the database view V_PRISONER_SCHEDULED_ACTIVITIES
 *
 * This view creates a join between:
 * - scheduled instances
 * - allocations
 * - activity schedules
 * - activities
 * And a LEFT join with
 * - activity suspensions
 *
 * Query clauses are added in the repository methods to restrict the rows returned from the
 * view by prison, dates, prisoners, and slot times.
 */

data class UniquePropertyId(val scheduledInstanceId: Long, val allocationId: Long) : Serializable {
  constructor() : this(-1, -1)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as UniquePropertyId

    if (scheduledInstanceId != other.scheduledInstanceId) return false
    if (allocationId != other.allocationId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = scheduledInstanceId.hashCode()
    result = 31 * result + allocationId.hashCode()
    return result
  }
}

@Entity
@Immutable
@Table(name = "v_prisoner_scheduled_activities")
@IdClass(UniquePropertyId::class)
data class PrisonerScheduledActivity(
  @Id
  val scheduledInstanceId: Long,

  @Id
  val allocationId: Long,

  val prisonCode: String,

  val sessionDate: LocalDate,

  val startTime: LocalTime? = null,

  val endTime: LocalTime? = null,

  val prisonerNumber: String,

  val bookingId: Int,

  val internalLocationId: Int? = null,

  val internalLocationCode: String? = null,

  val internalLocationDescription: String? = null,

  val scheduleDescription: String? = null,

  val activityId: Int,

  val activityCategory: String,

  val activitySummary: String? = null,

  val cancelled: Boolean = false,

  val suspended: Boolean = false,
)
