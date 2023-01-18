package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot as ModelActivityScheduleSlot

@Entity
@Table(name = "activity_schedule_slot")
data class ActivityScheduleSlot(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityScheduleSlotId: Long = -1,

  @ManyToOne
  @JoinColumn(name = "activity_schedule_id", nullable = false)
  val activitySchedule: ActivitySchedule,

  val startTime: LocalTime,

  val endTime: LocalTime,

  val mondayFlag: Boolean = false,

  val tuesdayFlag: Boolean = false,

  val wednesdayFlag: Boolean = false,

  val thursdayFlag: Boolean = false,

  val fridayFlag: Boolean = false,

  val saturdayFlag: Boolean = false,

  val sundayFlag: Boolean = false,

  val runsOnBankHoliday: Boolean = false,
) {
  init {
    failIfNoDaysSelectedForSlot()
    failIfDatesAreInvalidForSlot()
  }

  private fun failIfNoDaysSelectedForSlot() {
    if (mondayFlag.not() && tuesdayFlag.not() && wednesdayFlag.not() && thursdayFlag.not() && fridayFlag.not() && saturdayFlag.not() && sundayFlag.not()) {
      throw IllegalArgumentException("One or more days must be specified for a given slot.")
    }
  }

  private fun failIfDatesAreInvalidForSlot() {
    if (!endTime.isAfter(startTime)) {
      throw IllegalArgumentException("Start time '$startTime' must be before end time '$endTime'.")
    }
  }

  companion object {
    fun valueOf(
      activitySchedule: ActivitySchedule,
      startTime: LocalTime,
      endTime: LocalTime,
      daysOfWeek: Set<DayOfWeek>
    ) = ActivityScheduleSlot(
      activitySchedule = activitySchedule,
      startTime = startTime,
      endTime = endTime,
      mondayFlag = daysOfWeek.contains(DayOfWeek.MONDAY),
      tuesdayFlag = daysOfWeek.contains(DayOfWeek.TUESDAY),
      wednesdayFlag = daysOfWeek.contains(DayOfWeek.WEDNESDAY),
      thursdayFlag = daysOfWeek.contains(DayOfWeek.THURSDAY),
      fridayFlag = daysOfWeek.contains(DayOfWeek.FRIDAY),
      saturdayFlag = daysOfWeek.contains(DayOfWeek.SATURDAY),
      sundayFlag = daysOfWeek.contains(DayOfWeek.SUNDAY)
    )
  }

  fun toModel() = ModelActivityScheduleSlot(
    id = this.activityScheduleSlotId,
    startTime = this.startTime,
    endTime = this.endTime,
    daysOfWeek = this.getDaysOfWeek()
      .map { day -> day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) },
  )

  fun getDaysOfWeek(): List<DayOfWeek> = listOfNotNull(
    DayOfWeek.MONDAY.takeIf { mondayFlag },
    DayOfWeek.TUESDAY.takeIf { tuesdayFlag },
    DayOfWeek.WEDNESDAY.takeIf { wednesdayFlag },
    DayOfWeek.THURSDAY.takeIf { thursdayFlag },
    DayOfWeek.FRIDAY.takeIf { fridayFlag },
    DayOfWeek.SATURDAY.takeIf { saturdayFlag },
    DayOfWeek.SUNDAY.takeIf { sundayFlag }
  )

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(activityScheduleSlotId = $activityScheduleSlotId )"
  }
}

fun List<ActivityScheduleSlot>.toModelLite() = map { it.toModel() }
