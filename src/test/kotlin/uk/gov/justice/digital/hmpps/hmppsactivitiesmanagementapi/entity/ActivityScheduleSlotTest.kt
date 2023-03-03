package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.LocalTime

class ActivityScheduleSlotTest {

  @Test
  fun `conversion to model sets day flags and days-of-week list correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      activitySchedule = mock(),
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      mondayFlag = true,
      wednesdayFlag = true,
      fridayFlag = true,
      sundayFlag = true,
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
    }
  }

  @Test
  fun `conversion to model sets monday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      activitySchedule = mock(),
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      mondayFlag = true,
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
    }
  }

  @Test
  fun `conversion to model sets tuesday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      activitySchedule = mock(),
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      tuesdayFlag = true,
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
    }
  }

  @Test
  fun `conversion to model sets wednesday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      activitySchedule = mock(),
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      wednesdayFlag = true,
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
    }
  }

  @Test
  fun `conversion to model sets thursday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      activitySchedule = mock(),
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      thursdayFlag = true,
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
    }
  }

  @Test
  fun `conversion to model sets fridayFlag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      activitySchedule = mock(),
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      fridayFlag = true,
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
    }
  }

  @Test
  fun `conversion to model sets saturday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      activitySchedule = mock(),
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      saturdayFlag = true,
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
    }
  }

  @Test
  fun `conversion to model sets sunday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      activitySchedule = mock(),
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      sundayFlag = true,
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
    }
  }
}
