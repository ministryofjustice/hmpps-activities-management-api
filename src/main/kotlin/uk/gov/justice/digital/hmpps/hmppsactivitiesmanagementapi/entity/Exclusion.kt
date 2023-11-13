package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
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
  fun getDaysOfWeek(): Set<DayOfWeek> = setOfNotNull(
    DayOfWeek.MONDAY.takeIf { mondayFlag },
    DayOfWeek.TUESDAY.takeIf { tuesdayFlag },
    DayOfWeek.WEDNESDAY.takeIf { wednesdayFlag },
    DayOfWeek.THURSDAY.takeIf { thursdayFlag },
    DayOfWeek.FRIDAY.takeIf { fridayFlag },
    DayOfWeek.SATURDAY.takeIf { saturdayFlag },
    DayOfWeek.SUNDAY.takeIf { sundayFlag },
  )
  fun setDaysOfWeek(days: Set<DayOfWeek>) {
    if (!activityScheduleSlot.getDaysOfWeek().containsAll(days)) {
      throw IllegalArgumentException("Cannot set exclusions for slots where the activity does not run")
    }

    mondayFlag = days.contains(DayOfWeek.MONDAY)
    tuesdayFlag = days.contains(DayOfWeek.TUESDAY)
    wednesdayFlag = days.contains(DayOfWeek.WEDNESDAY)
    thursdayFlag = days.contains(DayOfWeek.THURSDAY)
    fridayFlag = days.contains(DayOfWeek.FRIDAY)
    saturdayFlag = days.contains(DayOfWeek.SATURDAY)
    sundayFlag = days.contains(DayOfWeek.SUNDAY)
  }
  fun getTimeSlot() = activityScheduleSlot.timeSlot()
  fun getWeekNumber() = activityScheduleSlot.weekNumber
  fun syncExcludedDaysWithSlot(slot: ActivityScheduleSlot) {
    mondayFlag = mondayFlag && slot.mondayFlag
    tuesdayFlag = tuesdayFlag && slot.tuesdayFlag
    wednesdayFlag = wednesdayFlag && slot.wednesdayFlag
    thursdayFlag = thursdayFlag && slot.thursdayFlag
    fridayFlag = fridayFlag && slot.fridayFlag
    saturdayFlag = saturdayFlag && slot.saturdayFlag
    sundayFlag = sundayFlag && slot.sundayFlag
  }
  fun toSlotModel() = Slot(
    weekNumber = getWeekNumber(),
    timeSlot = getTimeSlot().toString(),
    monday = mondayFlag,
    tuesday = tuesdayFlag,
    wednesday = wednesdayFlag,
    thursday = thursdayFlag,
    friday = fridayFlag,
    saturday = saturdayFlag,
    sunday = sundayFlag,
  )

  companion object {
    fun valueOf(
      allocation: Allocation,
      slot: ActivityScheduleSlot,
      daysOfWeek: Set<DayOfWeek>,
    ) = Exclusion(
      allocation = allocation,
      activityScheduleSlot = slot,
    ).apply {
      setDaysOfWeek(daysOfWeek)
    }
  }
}

fun List<Exclusion>.toSlotModel() = map { it.toSlotModel() }
