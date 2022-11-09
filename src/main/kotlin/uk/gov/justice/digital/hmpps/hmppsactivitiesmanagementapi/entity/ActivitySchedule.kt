package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "activity_schedule")
data class ActivitySchedule(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityScheduleId: Long? = null,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  @OneToMany(mappedBy = "activitySchedule", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  val instances: MutableList<ScheduledInstance> = mutableListOf(),

  @OneToMany(mappedBy = "activitySchedule", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  val suspensions: MutableList<ActivityScheduleSuspension> = mutableListOf(),

  @OneToMany(mappedBy = "activitySchedule", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  val allocations: MutableList<Allocation> = mutableListOf(),

  val description: String,

  val startTime: LocalTime,

  val endTime: LocalTime,

  var internalLocationId: Int? = null,

  var internalLocationCode: String? = null,

  var internalLocationDescription: String? = null,

  val capacity: Int,

  val mondayFlag: Boolean = false,

  val tuesdayFlag: Boolean = false,

  val wednesdayFlag: Boolean = false,

  val thursdayFlag: Boolean = false,

  val fridayFlag: Boolean = false,

  val saturdayFlag: Boolean = false,

  val sundayFlag: Boolean = false,
) {

  fun getDaysOfWeek(): List<DayOfWeek> = mutableListOf<DayOfWeek>().apply {
    if (mondayFlag) add(DayOfWeek.MONDAY)
    if (tuesdayFlag) add(DayOfWeek.TUESDAY)
    if (wednesdayFlag) add(DayOfWeek.WEDNESDAY)
    if (thursdayFlag) add(DayOfWeek.THURSDAY)
    if (fridayFlag) add(DayOfWeek.FRIDAY)
    if (saturdayFlag) add(DayOfWeek.SATURDAY)
    if (sundayFlag) add(DayOfWeek.SUNDAY)
  }

  fun getAllocationsOnDate(date: LocalDate): List<Allocation> = this.allocations.filter {
    !date.isBefore(it.startDate) && (it.endDate === null || !date.isAfter(it.endDate))
  }

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(activityScheduleId = $activityScheduleId )"
  }
}
