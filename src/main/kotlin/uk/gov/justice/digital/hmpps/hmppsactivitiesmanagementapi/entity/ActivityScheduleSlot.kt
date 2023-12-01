package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot as ModelActivityScheduleSlot

@Entity
@Table(name = "activity_schedule_slot")
data class ActivityScheduleSlot(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityScheduleSlotId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "activity_schedule_id", nullable = false)
  val activitySchedule: ActivitySchedule,

  val weekNumber: Int,

  val startTime: LocalTime,

  val endTime: LocalTime,

  var mondayFlag: Boolean = false,

  var tuesdayFlag: Boolean = false,

  var wednesdayFlag: Boolean = false,

  var thursdayFlag: Boolean = false,

  var fridayFlag: Boolean = false,

  var saturdayFlag: Boolean = false,

  var sundayFlag: Boolean = false,
) {
  init {
    failIfDatesAreInvalidForSlot()
    failIfWeekNumberInvalid()
  }

  private fun failIfDatesAreInvalidForSlot() {
    if (!endTime.isAfter(startTime)) {
      throw IllegalArgumentException("Start time '$startTime' must be before end time '$endTime'.")
    }
  }

  private fun failIfWeekNumberInvalid() {
    if (weekNumber <= 0) {
      throw IllegalArgumentException("Week number must be greater than zero.")
    }
    if (weekNumber > activitySchedule.scheduleWeeks) {
      throw IllegalArgumentException("Week number must less than or equal to the number of schedule weeks.")
    }
  }

  companion object {
    fun valueOf(
      activitySchedule: ActivitySchedule,
      weekNumber: Int,
      startTime: LocalTime,
      endTime: LocalTime,
      daysOfWeek: Set<DayOfWeek>,
    ) = ActivityScheduleSlot(
      activitySchedule = activitySchedule,
      weekNumber = weekNumber,
      startTime = startTime,
      endTime = endTime,
    ).apply {
      update(daysOfWeek)
    }
  }

  fun update(daysOfWeek: Set<DayOfWeek>) {
    require(daysOfWeek.isNotEmpty()) { "A slot must run on at least one day." }

    mondayFlag = daysOfWeek.contains(DayOfWeek.MONDAY)
    tuesdayFlag = daysOfWeek.contains(DayOfWeek.TUESDAY)
    wednesdayFlag = daysOfWeek.contains(DayOfWeek.WEDNESDAY)
    thursdayFlag = daysOfWeek.contains(DayOfWeek.THURSDAY)
    fridayFlag = daysOfWeek.contains(DayOfWeek.FRIDAY)
    saturdayFlag = daysOfWeek.contains(DayOfWeek.SATURDAY)
    sundayFlag = daysOfWeek.contains(DayOfWeek.SUNDAY)
  }

  fun toModel() = ModelActivityScheduleSlot(
    id = this.activityScheduleSlotId,
    weekNumber = this.weekNumber,
    startTime = this.startTime,
    endTime = this.endTime,
    daysOfWeek = this.getDaysOfWeek()
      .map { day -> day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) },
    mondayFlag = this.mondayFlag,
    tuesdayFlag = this.tuesdayFlag,
    wednesdayFlag = this.wednesdayFlag,
    thursdayFlag = this.thursdayFlag,
    fridayFlag = this.fridayFlag,
    saturdayFlag = this.saturdayFlag,
    sundayFlag = this.sundayFlag,
  )

  fun getDaysOfWeek(): Set<DayOfWeek> = setOfNotNull(
    DayOfWeek.MONDAY.takeIf { mondayFlag },
    DayOfWeek.TUESDAY.takeIf { tuesdayFlag },
    DayOfWeek.WEDNESDAY.takeIf { wednesdayFlag },
    DayOfWeek.THURSDAY.takeIf { thursdayFlag },
    DayOfWeek.FRIDAY.takeIf { fridayFlag },
    DayOfWeek.SATURDAY.takeIf { saturdayFlag },
    DayOfWeek.SUNDAY.takeIf { sundayFlag },
  )

  fun timeSlot() = TimeSlot.slot(startTime)

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(activityScheduleSlotId = $activityScheduleSlotId )"
  }
}

fun List<ActivityScheduleSlot>.toModelLite() = map { it.toModel() }
