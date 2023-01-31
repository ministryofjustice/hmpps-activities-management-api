package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import java.time.LocalDate

@Entity
@Table(name = "activity_schedule_suspension")
data class ActivityScheduleSuspension(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityScheduleSuspensionId: Long = -1,

  @ManyToOne
  @JoinColumn(name = "activity_schedule_id", nullable = false)
  val activitySchedule: ActivitySchedule,

  val suspendedFrom: LocalDate,

  // TODO this needs to be renamed to 'endDate', 'until' implies exclusive and we have been working with inclusive date ranges in other entities.
  val suspendedUntil: LocalDate? = null,
) {
  init {
    failIfSlotDateBoundariesAreInvalid()
  }

  private fun failIfSlotDateBoundariesAreInvalid() {
    if (suspendedUntil != null && suspendedUntil.isAfter(suspendedFrom).not()) {
      throw IllegalArgumentException("Until date must be after suspend from date")
    }

    if (suspendedFrom.isBefore(activitySchedule.startDate)) {
      throw IllegalArgumentException("Suspension dates must be the same or between the schedule dates")
    }

    if (suspendedUntil != null && activitySchedule.endDate != null && suspendedUntil.isAfter(activitySchedule.endDate)) {
      throw IllegalArgumentException("Suspension dates must be the same or between the schedule dates")
    }
  }

  fun isSuspendedOn(date: LocalDate) = date.between(suspendedFrom, suspendedUntil)
}
