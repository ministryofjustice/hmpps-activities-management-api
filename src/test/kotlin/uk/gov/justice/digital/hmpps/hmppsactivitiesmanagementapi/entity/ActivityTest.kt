package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import java.time.LocalDate

class ActivityTest {
  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val tomorrow = today.plusDays(1)

  private val activityWithNoEndDate = activityEntity().copy(startDate = today, endDate = null)

  private val activityWithEndDate = activityWithNoEndDate.copy(endDate = tomorrow)

  @Test
  fun `check activity active status that starts today with open end date`() {
    with(activityWithNoEndDate) {
      assertThat(isActive(yesterday)).isFalse
      assertThat(isActive(today)).isTrue
      assertThat(isActive(tomorrow)).isTrue
      assertThat(isActive(tomorrow.plusDays(1000))).isTrue
    }
  }

  @Test
  fun `check activity active status that starts today and ends tomorrow`() {
    with(activityWithEndDate) {
      assertThat(isActive(yesterday)).isFalse
      assertThat(isActive(today)).isTrue
      assertThat(isActive(tomorrow)).isTrue
      assertThat(isActive(tomorrow.plusDays(1))).isFalse
    }
  }

  @Test
  fun `converted to model lite`() {
    val expectedModel = ActivityLite(
      id = 1,
      attendanceRequired = false,
      inCell = false,
      pieceWork = false,
      outsideWork = false,
      payPerSession = PayPerSession.H,
      prisonCode = "123",
      summary = "Maths",
      description = "Maths basic",
      riskLevel = "High",
      minimumIncentiveLevel = "Basic",
      category = ActivityCategory(
        id = 1L,
        code = "category code",
        name = "category name",
        description = "category description"
      )
    )
    assertThat(activityEntity().copy(attendanceRequired = false).toModelLite()).isEqualTo(expectedModel)
  }

  @Test
  fun `List converted to model lite`() {
    val expectedModel = listOf(
      ActivityLite(
        id = 1,
        attendanceRequired = true,
        inCell = false,
        pieceWork = false,
        outsideWork = false,
        payPerSession = PayPerSession.H,
        prisonCode = "123",
        summary = "Maths",
        description = "Maths basic",
        riskLevel = "High",
        minimumIncentiveLevel = "Basic",
        category = ActivityCategory(
          id = 1L,
          code = "category code",
          name = "category name",
          description = "category description"
        )
      )
    )

    assertThat(listOf(activityEntity()).toModelLite()).isEqualTo(expectedModel)
  }

  @Test
  fun `can add schedule to activity`() {
    val activity = activityEntity(noSchedules = true)
    assertThat(activity.schedules()).isEmpty()

    activity.addSchedule(
      description = "Woodwork",
      internalLocationId = 1,
      internalLocationCode = "WW",
      internalLocationDescription = "The wood work room description",
      capacity = 10,
      startDate = activity.startDate
    )

    assertThat(activity.schedules()).containsExactly(
      ActivitySchedule(
        activity = activity,
        description = "Woodwork",
        internalLocationId = 1,
        internalLocationCode = "WW",
        internalLocationDescription = "The wood work room description",
        capacity = 10,
        startDate = activity.startDate
      )
    )
  }

  @Test
  fun `cannot add schedule when start date is before that of the activity`() {
    val activity = activityEntity(noSchedules = true)
    assertThat(activity.schedules()).isEmpty()

    assertThatThrownBy {
      activity.addSchedule(
        description = "Woodwork",
        internalLocationId = 1,
        internalLocationCode = "WW",
        internalLocationDescription = "The wood work room description",
        capacity = 10,
        startDate = activity.startDate.minusDays(1)
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The schedule start date '${activity.startDate.minusDays(1)}' cannot be before the activity start date ${activity.startDate}")
  }

  @Test
  fun `cannot add schedule when start date is not before end date of the activity`() {
    val activity =
      activityEntity(noSchedules = true).copy(startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1))
    assertThat(activity.schedules()).isEmpty()

    assertThatThrownBy {
      activity.addSchedule(
        description = "Woodwork",
        internalLocationId = 1,
        internalLocationCode = "WW",
        internalLocationDescription = "The wood work room description",
        capacity = 10,
        startDate = activity.endDate!!
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The schedule start date '${activity.endDate}' must be before the activity end date ${activity.endDate}")
  }

  @Test
  fun `cannot add schedule when end date is after the end date of the activity`() {
    val activity =
      activityEntity(noSchedules = true).copy(startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1))
    assertThat(activity.schedules()).isEmpty()

    assertThatThrownBy {
      activity.addSchedule(
        description = "Woodwork",
        internalLocationId = 1,
        internalLocationCode = "WW",
        internalLocationDescription = "The wood work room description",
        capacity = 10,
        startDate = activity.startDate,
        endDate = activity.endDate!!.plusDays(1)
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The schedule end date '${activity.endDate!!.plusDays(1)}' cannot be after the activity end date ${activity.endDate}")
  }

  @Test
  fun `cannot add schedule when capacity less than 1`() {
    val activity = activityEntity(noSchedules = true)
    assertThat(activity.schedules()).isEmpty()

    assertThatThrownBy {
      activity.addSchedule(
        description = "Woodwork",
        internalLocationId = 1,
        internalLocationCode = "WW",
        internalLocationDescription = "The wood work room description",
        capacity = 0,
        startDate = activity.startDate,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The schedule capacity must be greater than zero.")
  }

  @Test
  fun `cannot add schedule with the same description`() {
    val activity = activityEntity(noSchedules = true)
    assertThat(activity.schedules()).isEmpty()

    activity.addSchedule(
      description = "Woodwork",
      internalLocationId = 1,
      internalLocationCode = "WW",
      internalLocationDescription = "The wood work room description",
      capacity = 10,
      startDate = activity.startDate
    )

    assertThatThrownBy {
      activity.addSchedule(
        description = " WooDwork ",
        internalLocationId = 2,
        internalLocationCode = "WW2",
        internalLocationDescription = "The wood work room description 2",
        capacity = 10,
        startDate = activity.startDate
      )
    }
  }

  @Test
  fun `cannot add schedule belonging to another activity`() {
    val activity = activityEntity()

    val scheduleBelongingToDifferentActivity =
      activitySchedule(activity.copy(activityId = 99), startDate = activity.startDate, description = "other activity")

    assertThatThrownBy {
      activity.addSchedule(scheduleBelongingToDifferentActivity)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Can only add schedules that belong to this activity.")
  }
}
