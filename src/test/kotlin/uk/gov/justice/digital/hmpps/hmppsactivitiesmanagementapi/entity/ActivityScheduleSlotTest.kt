package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import java.time.DayOfWeek
import java.time.LocalTime

class ActivityScheduleSlotTest {
  private val activity = activityEntity()
  private val activitySchedule = activity.schedules().first()
  private val startTime = LocalTime.of(9, 0)
  private val endTime = startTime.plusHours(2)
  private val timeSlot = TimeSlot.AM

  @Test
  fun `conversion to model sets day flags and days-of-week list correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = startTime,
      endTime = endTime,
      mondayFlag = true,
      wednesdayFlag = true,
      fridayFlag = true,
      sundayFlag = true,
      timeSlot = timeSlot,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isTrue
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isTrue
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isTrue
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isTrue
      assertThat(daysOfWeek).containsExactly("Mon", "Wed", "Fri", "Sun")
      assertThat(timeSlot).isEqualTo(timeSlot)
    }
  }

  @Test
  fun `conversion to model sets monday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = startTime,
      endTime = endTime,
      mondayFlag = true,
      timeSlot = timeSlot,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isTrue
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isFalse
      assertThat(daysOfWeek).containsExactly("Mon")
      assertThat(timeSlot).isEqualTo(timeSlot)
    }
  }

  @Test
  fun `conversion to model sets tuesday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = startTime,
      endTime = endTime,
      tuesdayFlag = true,
      timeSlot = timeSlot,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isTrue
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isFalse
      assertThat(daysOfWeek).containsExactly("Tue")
      assertThat(timeSlot).isEqualTo(timeSlot)
    }
  }

  @Test
  fun `conversion to model sets wednesday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = startTime,
      endTime = endTime,
      wednesdayFlag = true,
      timeSlot = timeSlot,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isTrue
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isFalse
      assertThat(daysOfWeek).containsExactly("Wed")
      assertThat(timeSlot).isEqualTo(timeSlot)
    }
  }

  @Test
  fun `conversion to model sets thursday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = startTime,
      endTime = endTime,
      thursdayFlag = true,
      timeSlot = timeSlot,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isTrue
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isFalse
      assertThat(daysOfWeek).containsExactly("Thu")
      assertThat(timeSlot).isEqualTo(timeSlot)
    }
  }

  @Test
  fun `conversion to model sets fridayFlag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = startTime,
      endTime = endTime,
      fridayFlag = true,
      timeSlot = timeSlot,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isTrue
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isFalse
      assertThat(daysOfWeek).containsExactly("Fri")
      assertThat(timeSlot).isEqualTo(timeSlot)
    }
  }

  @Test
  fun `conversion to model sets saturday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = startTime,
      endTime = endTime,
      saturdayFlag = true,
      timeSlot = timeSlot,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isTrue
      assertThat(sundayFlag).isFalse
      assertThat(daysOfWeek).containsExactly("Sat")
      assertThat(timeSlot).isEqualTo(timeSlot)
    }
  }

  @Test
  fun `conversion to model sets sunday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = startTime,
      endTime = endTime,
      sundayFlag = true,
      timeSlot = timeSlot,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isTrue
      assertThat(daysOfWeek).containsExactly("Sun")
      assertThat(timeSlot).isEqualTo(timeSlot)
    }
  }

  @Test
  fun `change day of slot`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = startTime,
      endTime = endTime,
      mondayFlag = true,
      sundayFlag = true,
      timeSlot = timeSlot,
    )

    with(slot) {
      assertThat(mondayFlag).isTrue
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isTrue
    }

    slot.update(setOf(DayOfWeek.TUESDAY))

    with(slot) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isTrue
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isFalse
      assertThat(timeSlot).isEqualTo(timeSlot)
    }
  }

  @Test
  fun `must provide at least one day on update`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = startTime,
      endTime = endTime,
      mondayFlag = true,
      sundayFlag = true,
      timeSlot = timeSlot,
    )

    assertThatThrownBy { slot.update(emptySet()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("A slot must run on at least one day.")
  }

  @Test
  fun `fails to initialise if week number is not greater than zero`() {
    assertThatThrownBy {
      ActivityScheduleSlot(
        activityScheduleSlotId = 1,
        weekNumber = 0,
        activitySchedule = activitySchedule,
        startTime = startTime,
        endTime = endTime,
        mondayFlag = true,
        sundayFlag = true,
        timeSlot = timeSlot,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Week number must be greater than zero.")

    assertThatThrownBy {
      ActivityScheduleSlot(
        activityScheduleSlotId = 1,
        weekNumber = -1,
        activitySchedule = activitySchedule,
        startTime = startTime,
        endTime = endTime,
        mondayFlag = true,
        sundayFlag = true,
        timeSlot = timeSlot,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Week number must be greater than zero.")
  }

  @Test
  fun `fails to initialise if week number is not in range`() {
    val activity = activityEntity()
    val schedule = activity.schedules().first().apply { scheduleWeeks = 2 }

    assertThatThrownBy {
      ActivityScheduleSlot(
        activityScheduleSlotId = 1,
        weekNumber = 3,
        activitySchedule = schedule,
        startTime = startTime,
        endTime = endTime,
        mondayFlag = true,
        sundayFlag = true,
        timeSlot = timeSlot,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Week number must less than or equal to the number of schedule weeks.")
  }
}
