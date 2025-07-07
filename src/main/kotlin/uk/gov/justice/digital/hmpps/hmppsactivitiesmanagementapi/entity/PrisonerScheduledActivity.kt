package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ScheduledAttendee
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

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

  val bookingId: Long,

  val inCell: Boolean,

  val onWing: Boolean,

  val offWing: Boolean,

  val internalLocationId: Int? = null,

  val dpsLocationId: UUID? = null,

  val internalLocationCode: String? = null,

  val internalLocationDescription: String? = null,

  val scheduleDescription: String? = null,

  val activityId: Int,

  val activityCategory: String,

  val activitySummary: String? = null,

  val cancelled: Boolean = false,

  val suspended: Boolean = false,

  val autoSuspended: Boolean = false,

  @Enumerated(EnumType.STRING)
  val timeSlot: TimeSlot,

  val issuePayment: Boolean?,

  @Enumerated(EnumType.STRING)
  val attendanceStatus: AttendanceStatus?,

  @Enumerated(EnumType.STRING)
  val attendanceReasonCode: AttendanceReasonEnum?,

  val paidActivity: Boolean,

  val possibleAdvanceAttendance: Boolean,
) {
  fun toScheduledAttendeeModel() = ScheduledAttendee(
    scheduledInstanceId = scheduledInstanceId,
    allocationId = allocationId,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    suspended = suspended,
    autoSuspended = autoSuspended,
  )

  fun toScheduledActivityModel() = ScheduledActivity(
    scheduledInstanceId = scheduledInstanceId,
    allocationId = allocationId,
    prisonCode = prisonCode,
    sessionDate = sessionDate,
    startTime = startTime,
    endTime = endTime,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    inCell = inCell,
    onWing = onWing,
    offWing = offWing,
    internalLocationId = internalLocationId,
    internalLocationDescription = internalLocationDescription,
    scheduleDescription = scheduleDescription,
    activityId = activityId,
    activityCategory = activityCategory,
    activitySummary = activitySummary,
    cancelled = cancelled,
    suspended = suspended,
    autoSuspended = autoSuspended,
    timeSlot = timeSlot,
    issuePayment = issuePayment,
    attendanceStatus = attendanceStatus,
    attendanceReasonCode = attendanceReasonCode,
    paidActivity = paidActivity,
    possibleAdvanceAttendance = possibleAdvanceAttendance,
  )
}

fun List<PrisonerScheduledActivity>.toScheduledAttendeeModel() = map { it.toScheduledAttendeeModel() }

fun List<PrisonerScheduledActivity>.toScheduledActivityModel() = map { it.toScheduledActivityModel() }
