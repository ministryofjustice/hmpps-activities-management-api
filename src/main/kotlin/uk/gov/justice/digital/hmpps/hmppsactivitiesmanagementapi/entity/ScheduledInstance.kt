package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance as ModelScheduledInstance

@Entity
@Table(name = "scheduled_instance")
data class ScheduledInstance(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val scheduledInstanceId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "activity_schedule_id", nullable = false)
  val activitySchedule: ActivitySchedule,

  @OneToMany(mappedBy = "scheduledInstance", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  val attendances: MutableList<Attendance> = mutableListOf(),

  val sessionDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime,

  var cancelled: Boolean = false,

  var cancelledTime: LocalDateTime? = null,

  var cancelledBy: String? = null,

  var cancelledReason: String? = null,

  var comment: String? = null,

  @Enumerated(EnumType.STRING)
  var timeSlot: TimeSlot,

  var cancelledIssuePayment: Boolean? = null,
) {

  fun dayOfWeek() = sessionDate.dayOfWeek

  fun toModel(includeAllocations: Boolean = true) = ModelScheduledInstance(
    activitySchedule = this.activitySchedule.toModelLite(includeAllocations),
    id = this.scheduledInstanceId,
    date = this.sessionDate,
    startTime = this.startTime,
    endTime = this.endTime,
    cancelled = this.cancelled,
    cancelledTime = this.cancelledTime,
    cancelledBy = this.cancelledBy,
    cancelledReason = this.cancelledReason,
    cancelledIssuePayment = this.cancelledIssuePayment,
    comment = this.comment,
    previousScheduledInstanceId = if (includeAllocations) this.previous()?.scheduledInstanceId else null,
    previousScheduledInstanceDate = if (includeAllocations) this.previous()?.sessionDate else null,
    nextScheduledInstanceId = if (includeAllocations) this.next()?.scheduledInstanceId else null,
    nextScheduledInstanceDate = if (includeAllocations) this.next()?.sessionDate else null,
    attendances = this.attendances.map { attendance -> transform(attendance, null) },
    timeSlot = this.timeSlot,
  )

  private fun previous() = activitySchedule.previous(this)

  private fun next() = activitySchedule.next(this)

  fun isRunningOn(date: LocalDate) = !cancelled && sessionDate == date

  fun slotTimes() = startTime to endTime

  fun attendanceRequired() = activitySchedule.activity.attendanceRequired

  /**
   * This will only cancel Not recorded or attendances marked as attended.
   * To cancel a suspended attendance, then the attendance must be cancelled directly.
   * Returns the attendances that have been cancelled.
   */
  fun cancelSessionAndAttendances(
    reason: String,
    by: String,
    cancelComment: String?,
    cancellationReason: AttendanceReason,
    issuePayment: Boolean? = null,
    useNewPriorityRules: Boolean = false,
  ): List<Attendance> {
    require(!cancelled) { "${activitySchedule.description} ($timeSlot) has already been cancelled" }

    val today = LocalDateTime.now().withNano(0)

    require(sessionDate >= today.toLocalDate()) { "${activitySchedule.description} ($timeSlot) has ended" }

    cancelled = true
    cancelledReason = reason
    cancelledTime = today
    cancelledBy = by
    comment = cancelComment
    cancelledIssuePayment = issuePayment

    if (useNewPriorityRules) {
      return attendances
        .filter { it.hasReason(AttendanceReasonEnum.ATTENDED) || it.attendanceReason == null }
        .onEach {
          it.cancel(
            reason = cancellationReason,
            cancelledReason = reason,
            cancelledBy = by,
            issuePayment = issuePayment,
          )
        }
    }

    return attendances
      .filterNot { it.hasReason(AttendanceReasonEnum.SUSPENDED, AttendanceReasonEnum.AUTO_SUSPENDED) }
      .onEach { it.cancel(reason = cancellationReason, cancelledReason = reason, cancelledBy = by, issuePayment = issuePayment) }
  }

  /**
   * Update a cancelled session and attendances and return the updated attendances
   */
  fun updateCancelledSessionAndAttendances(
    reason: String?,
    updatedBy: String,
    cancelComment: String?,
    issuePayment: Boolean? = null,
  ): List<Attendance> {
    require(cancelled) { "Cannot update ${activitySchedule.description} ($timeSlot) because it is not cancelled" }

    require(issuePayment != true || activitySchedule.activity.isPaid()) { "Cannot issue payment for ${activitySchedule.description} ($timeSlot) because it is not payable" }

    val now = LocalDateTime.now().withNano(0)

    require(sessionDate >= now.toLocalDate()) { "Cannot update ${activitySchedule.description} ($timeSlot) has ended" }

    if (reason != null) {
      cancelledReason = reason
      comment = cancelComment
      cancelledBy = updatedBy
      cancelledTime = now
    }

    cancelledIssuePayment = issuePayment ?: cancelledIssuePayment

    return attendances
      .filterNot { issuePayment == null }
      .filter { it.hasReason(AttendanceReasonEnum.CANCELLED) }
      .onEach { it.updateCancelledAttendance(reason, updatedBy, issuePayment!!) }
  }

  /**
   * This will not uncancel suspended attendances. If you wish to uncancel a suspended attendance then you must uncancel the
   * attendance directly. Returns the attendances that have been uncancelled.
   */
  fun uncancelSessionAndAttendances(useNewPriorityRules: Boolean = false): List<Attendance> {
    require(sessionDate >= LocalDate.now()) {
      "Cannot uncancel scheduled instance [$scheduledInstanceId] because it is in the past"
    }

    require(cancelled) { "Cannot uncancel scheduled instance [$scheduledInstanceId] because it is not cancelled" }

    cancelled = false
    cancelledBy = null
    cancelledReason = null
    cancelledTime = null
    cancelledIssuePayment = null

    if (useNewPriorityRules) {
      return attendances
        .filter { it.hasReason(AttendanceReasonEnum.CANCELLED) }
        .onEach(Attendance::uncancel)
    }

    return attendances
      .filterNot { it.hasReason(AttendanceReasonEnum.SUSPENDED, AttendanceReasonEnum.AUTO_SUSPENDED) }
      .onEach(Attendance::uncancel)
  }

  fun remove(attendance: Attendance) {
    require(attendances.contains(attendance)) { "Attendance record with ${attendance.attendanceId} does not exist on the scheduled instance" }

    attendances.remove(attendance)
  }

  fun isPaid() = activitySchedule.isPaid()

  fun activitySummary() = activitySchedule.activity.summary

  fun internalLocationDescription() = activitySchedule.internalLocationDescription

  fun dateTime() = sessionDate.atTime(startTime)

  fun isFuture(dateTime: LocalDateTime) = sessionDate.atTime(startTime).isAfter(dateTime)

  fun isEndFuture(dateTime: LocalDateTime) = sessionDate.atTime(endTime).isAfter(dateTime)
}

fun List<ScheduledInstance>.toModel(includeAllocations: Boolean = true) = map { it.toModel(includeAllocations) }
