package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import java.time.LocalDate
import java.time.LocalTime

class ActivityScheduleTest {
  @Test
  fun `get allocations for date`() {
    val schedule = activitySchedule(activity = activityEntity()).apply {
      val decemberAllocation = Allocation(
        allocationId = 1,
        activitySchedule = this,
        prisonerNumber = "A1234AA",
        startDate = LocalDate.parse("2022-12-01"),
        endDate = null,
      )

      val novemberAllocation = Allocation(
        allocationId = 2,
        activitySchedule = this,
        prisonerNumber = "A1234AA",
        startDate = LocalDate.parse("2022-11-10"),
        endDate = LocalDate.parse("2022-11-30"),
      )

      allocations.clear()
      allocations.add(novemberAllocation)
      allocations.add(decemberAllocation)
    }

    assertThat(schedule.getAllocationsOnDate(LocalDate.parse("2022-10-01"))).isEqualTo(emptyList<Allocation>())
    assertThat(schedule.getAllocationsOnDate(LocalDate.parse("2022-11-10"))[0].allocationId).isEqualTo(2)
    assertThat(schedule.getAllocationsOnDate(LocalDate.parse("2022-11-30"))[0].allocationId).isEqualTo(2)
    assertThat(schedule.getAllocationsOnDate(LocalDate.parse("2022-12-01"))[0].allocationId).isEqualTo(1)
    assertThat(schedule.getAllocationsOnDate(LocalDate.parse("2025-01-01"))[0].allocationId).isEqualTo(1)
  }

  @Test
  fun `converted to model lite`() {

    val expectedModel = ActivityScheduleLite(
      id = 1,
      description = "schedule description",
      startTime = LocalTime.of(10, 20),
      endTime = LocalTime.of(10, 20),
      internalLocation = InternalLocation(1, "EDU-ROOM-1", "Education - R1"),
      daysOfWeek = listOf("Mon"),
      capacity = 1,
      activity = ActivityLite(
        id = 1L,
        prisonCode = "123",
        summary = "Maths",
        description = "Maths basic"
      )
    )
    assertThat(
      activitySchedule(
        activityEntity(),
        LocalDate.now().atTime(10, 20)
      ).toModelLite()
    ).isEqualTo(expectedModel)
  }

  @Test
  fun `List converted to model lite`() {
    val expectedModel = listOf(
      ActivityScheduleLite(
        id = 1,
        description = "schedule description",
        startTime = LocalTime.of(10, 20),
        endTime = LocalTime.of(10, 20),
        internalLocation = InternalLocation(1, "EDU-ROOM-1", "Education - R1"),
        daysOfWeek = listOf("Mon"),
        capacity = 1,
        activity = ActivityLite(
          id = 1L,
          prisonCode = "123",
          summary = "Maths",
          description = "Maths basic"
        )
      )
    )

    assertThat(listOf(activitySchedule(activityEntity(), LocalDate.now().atTime(10, 20))).toModelLite()).isEqualTo(
      expectedModel
    )
  }
}
