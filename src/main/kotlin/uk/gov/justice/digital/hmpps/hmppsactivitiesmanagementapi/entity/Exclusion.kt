package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.DayOfWeek

@Entity
@Table(name = "exclusion")
data class Exclusion(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val exclusionId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "allocation_id", nullable = false)
  val allocation: Allocation,

  @ManyToOne
  @JoinColumn(name = "activity_schedule_slot_id", nullable = false)
  val activityScheduleSlot: ActivityScheduleSlot,

  var mondayFlag: Boolean = false,

  var tuesdayFlag: Boolean = false,

  var wednesdayFlag: Boolean = false,

  var thursdayFlag: Boolean = false,

  var fridayFlag: Boolean = false,

  var saturdayFlag: Boolean = false,

  var sundayFlag: Boolean = false,
) {
  fun getDaysOfWeek(): List<DayOfWeek> = listOfNotNull(
    DayOfWeek.MONDAY.takeIf { mondayFlag },
    DayOfWeek.TUESDAY.takeIf { tuesdayFlag },
    DayOfWeek.WEDNESDAY.takeIf { wednesdayFlag },
    DayOfWeek.THURSDAY.takeIf { thursdayFlag },
    DayOfWeek.FRIDAY.takeIf { fridayFlag },
    DayOfWeek.SATURDAY.takeIf { saturdayFlag },
    DayOfWeek.SUNDAY.takeIf { sundayFlag },
  )
  fun getTimeSlot() = activityScheduleSlot.timeSlot()
  fun getWeekNumber() = activityScheduleSlot.weekNumber

  companion object {
    fun valueOf(
      allocation: Allocation,
      slot: ActivityScheduleSlot,
      daysOfWeek: Set<DayOfWeek>,
    ) = Exclusion(
      allocation = allocation,
      activityScheduleSlot = slot,
      mondayFlag = daysOfWeek.contains(DayOfWeek.MONDAY),
      tuesdayFlag = daysOfWeek.contains(DayOfWeek.TUESDAY),
      wednesdayFlag = daysOfWeek.contains(DayOfWeek.WEDNESDAY),
      thursdayFlag = daysOfWeek.contains(DayOfWeek.THURSDAY),
      fridayFlag = daysOfWeek.contains(DayOfWeek.FRIDAY),
      saturdayFlag = daysOfWeek.contains(DayOfWeek.SATURDAY),
      sundayFlag = daysOfWeek.contains(DayOfWeek.SUNDAY),
    )
  }
}
