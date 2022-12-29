package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect

/*
 * Read-only entity for the database view V_PRISONER_SCHEDULED_ACTIVITIES
 *
 * This view creates a join between:
 * - scheduled instances
 * - allocations
 * - activity schedules
 * - activities
 * And an outer join with
 * - activity suspensions
 *
 * Where clauses are added in the repository methods to restrict the rows returned from the
 * view by prison, dates, prisoners, and slot times and to check that the allocation dates
 * are within the dates specified, if provided.
 */

// Composite @Id - makes the rows have a unique identifier
class UniquePropertyId(
  val scheduledInstanceId: Long? = null,
  val allocationId: Long? = null,
) : Serializable

@Entity
@Immutable
@Table(name = "v_prisoner_scheduled_activities")
@IdClass(UniquePropertyId::class)
data class PrisonerScheduledActivity(

  @Id
  val scheduledInstanceId: Long? = null,

  @Id
  val allocationId: Long? = null,

  val prisonCode: String? = null,

  val sessionDate: LocalDate? = null,

  val startTime: LocalTime? = null,

  val endTime: LocalTime? = null,

  val prisonerNumber: String? = null,

  val bookingId: Int? = null,

  val internalLocationId: Int? = null,

  val internalLocationCode: String? = null,

  val internalLocationDescription: String? = null,

  val scheduleDescription: String? = null,

  val activityId: Int? = null,

  val activityCategory: String? = null,

  val activitySummary: String? = null,

  val cancelled: Boolean? = null,

  val suspended: Boolean? = null,
)
