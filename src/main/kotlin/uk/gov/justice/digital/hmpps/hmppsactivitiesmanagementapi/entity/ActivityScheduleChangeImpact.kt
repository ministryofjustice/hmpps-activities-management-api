package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Records that a specific allocation was actually affected by an activity schedule slot amendment
 * (i.e. a session was added that the prisoner will now attend, or a session was removed that the
 * prisoner was attending). Only affected allocations get a row - see ActivityService.applySlotsUpdate().
 *
 * The added/removed sessions are stored as JSON since they simply describe what changed for display
 * purposes and are not queried on.
 */
@Entity
@Table(name = "activity_schedule_change_impact")
class ActivityScheduleChangeImpact(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityScheduleChangeImpactId: Long = 0,

  val activityId: Long,

  val activityScheduleId: Long,

  val allocationId: Long,

  val prisonerNumber: String,

  val changedAt: LocalDateTime,

  val changedBy: String,

  @Column(name = "added_sessions")
  val addedSessionsJson: String? = null,

  @Column(name = "removed_sessions")
  val removedSessionsJson: String? = null,
)
