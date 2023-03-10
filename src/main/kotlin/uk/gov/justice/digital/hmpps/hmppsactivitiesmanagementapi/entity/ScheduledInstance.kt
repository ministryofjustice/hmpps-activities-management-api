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
  val scheduledInstanceId: Long = -1,

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
  fun toModel() = ModelScheduledInstance(
    activitySchedule = this.activitySchedule.toModelLite(),
    id = this.scheduledInstanceId,
    date = this.sessionDate,
    startTime = this.startTime,
    endTime = this.endTime,
    cancelled = this.cancelled,
    cancelledTime = this.cancelledTime,
    cancelledBy = this.cancelledBy,
    attendances = this.attendances.map { attendance -> transform(attendance) },
  )

  fun isRunningOn(date: LocalDate) = !cancelled && sessionDate == date

  fun timeSlot() = TimeSlot.slot(startTime)

  fun attendanceRequired() = activitySchedule.activity.attendanceRequired
}

fun List<ScheduledInstance>.toModel() = map { it.toModel() }
