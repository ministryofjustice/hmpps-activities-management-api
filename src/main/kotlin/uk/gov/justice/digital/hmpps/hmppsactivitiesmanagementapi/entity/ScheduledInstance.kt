package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
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

  @OneToMany(mappedBy = "scheduledInstance", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  val attendances: MutableList<Attendance> = mutableListOf(),

  val sessionDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime,

  var cancelled: Boolean = false,

  var cancelledTime: LocalDateTime? = null,

  var cancelledBy: String? = null,

  var cancelledReason: String? = null,

  var comment: String? = null,
) {

  fun uncancel() {
    if (sessionDate.isBefore(LocalDate.now())) {
      throw IllegalArgumentException("Cannot uncancel scheduled instance [$scheduledInstanceId] because it is in the past")
    }

    if (!cancelled) {
      throw IllegalArgumentException("Cannot uncancel scheduled instance [$scheduledInstanceId] because it is not cancelled")
    }

    cancelled = false
    cancelledBy = null
    cancelledReason = null
    attendances.forEach(Attendance::uncancel)
  }

  fun dayOfWeek() = sessionDate.dayOfWeek

  fun toModel() = ModelScheduledInstance(
    activitySchedule = this.activitySchedule.toModelLite(),
    id = this.scheduledInstanceId,
    date = this.sessionDate,
    startTime = this.startTime,
    endTime = this.endTime,
    cancelled = this.cancelled,
    cancelledTime = this.cancelledTime,
    cancelledBy = this.cancelledBy,
    cancelledReason = this.cancelledReason,
    comment = this.comment,
    previousScheduledInstanceId = this.previous()?.scheduledInstanceId,
    previousScheduledInstanceDate = this.previous()?.sessionDate,
    nextScheduledInstanceId = this.next()?.scheduledInstanceId,
    nextScheduledInstanceDate = this.next()?.sessionDate,
    attendances = this.attendances.map { attendance -> transform(attendance, null) },
  )

  private fun previous() = activitySchedule.previous(this)

  private fun next() = activitySchedule.next(this)

  fun isRunningOn(date: LocalDate) = !cancelled && sessionDate == date

  fun timeSlot() = TimeSlot.slot(startTime)

  fun attendanceRequired() = activitySchedule.activity.attendanceRequired

  fun cancelSession(reason: String, by: String, cancelComment: String?, f: (List<Attendance>) -> Unit) {
    if (cancelled) throw IllegalArgumentException("The schedule instance has already been cancelled")

    val today = LocalDateTime.now().withNano(0)
    if (sessionDate < today.toLocalDate()) throw IllegalArgumentException("The schedule instance has ended")

    cancelled = true
    cancelledReason = reason
    cancelledTime = today
    cancelledBy = by
    comment = cancelComment
    if (attendances.isNotEmpty()) f(attendances)
  }
}

fun List<ScheduledInstance>.toModel() = map { it.toModel() }
