package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

class ActivityScheduleTest {
  @Test
  fun `get allocations for date`() {
    val schedule = activitySchedule(activity = activityEntity()).apply {
      val decemberAllocation = Allocation(
        allocationId = 1,
        activitySchedule = this,
        prisonerNumber = "A1234AA",
        startDate = LocalDate.of(2022, 12, 1),
        endDate = null,
        allocatedBy = "FAKE USER",
        allocatedTime = LocalDate.of(2022, 12, 1).atStartOfDay()
      )

      val novemberAllocation = Allocation(
        allocationId = 2,
        activitySchedule = this,
        prisonerNumber = "A1234AA",
        startDate = LocalDate.of(2022, 11, 10),
        endDate = LocalDate.of(2022, 11, 30),
        allocatedBy = "FAKE USER",
        allocatedTime = LocalDate.of(2022, 11, 10).atStartOfDay()
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
        attendanceRequired = true,
        prisonCode = "123",
        summary = "Maths",
        description = "Maths basic",
        riskLevel = "HIGH",
        incentiveLevel = "BAS",
        category = ModelActivityCategory(
          id = 1L,
          code = "category code",
          name = "category name",
          description = "category description"
        )
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
          attendanceRequired = true,
          prisonCode = "123",
          summary = "Maths",
          description = "Maths basic",
          riskLevel = "HIGH",
          incentiveLevel = "BAS",
          category = ModelActivityCategory(
            id = 1L,
            code = "category code",
            name = "category name",
            description = "category description"
          )
        )
      )
    )

    assertThat(listOf(activitySchedule(activityEntity(), LocalDate.now().atTime(10, 20))).toModelLite()).isEqualTo(
      expectedModel
    )
  }

  @Test
  fun `can allocate prisoner to a schedule with no allocations`() {
    val schedule = activityEntity().schedules
      .first()
      .apply { allocations.clear() }
      .also { assertThat(it.allocations).isEmpty() }

    schedule.allocatePrisoner(
      prisonerNumber = "123456".toPrisonerNumber(),
      payBand = "A".toPayBand(),
    )

    assertThat(schedule.allocations).hasSize(1)

    with(schedule.allocations.first()) {
      assertThat(activitySchedule).isEqualTo(schedule)
      assertThat(prisonerNumber).isEqualTo("123456")
      assertThat(payBand).isEqualTo("A")
      assertThat(startDate).isEqualTo(LocalDate.now())
      assertThat(allocatedBy).isEqualTo("SYSTEM")
      assertThat(allocatedTime).isEqualToIgnoringSeconds(LocalDateTime.now())
    }
  }

  @Test
  fun `can allocate prisoner to a schedule with existing allocation`() {
    val schedule = activityEntity().schedules
      .first()
      .also { assertThat(it.allocations).hasSize(1) }

    schedule.allocatePrisoner(
      prisonerNumber = "654321".toPrisonerNumber(),
      payBand = "B".toPayBand(),
    )

    assertThat(schedule.allocations).hasSize(2)

    with(schedule.allocations.first { it.prisonerNumber == "654321" }) {
      assertThat(activitySchedule).isEqualTo(schedule)
      assertThat(prisonerNumber).isEqualTo("654321")
      assertThat(payBand).isEqualTo("B")
      assertThat(startDate).isEqualTo(LocalDate.now())
      assertThat(allocatedBy).isEqualTo("SYSTEM")
      assertThat(allocatedTime).isEqualToIgnoringSeconds(LocalDateTime.now())
    }
  }

  @Test
  fun `cannot allocate prisoner if already allocated to schedule`() {
    val schedule = activityEntity().schedules
      .first()
      .apply { allocations.clear() }
      .also { assertThat(it.allocations).isEmpty() }

    schedule.allocatePrisoner(
      prisonerNumber = "654321".toPrisonerNumber(),
      payBand = "B".toPayBand(),
    )

    assertThatThrownBy {
      schedule.allocatePrisoner(
        prisonerNumber = "654321".toPrisonerNumber(),
        payBand = "B".toPayBand(),
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
  }
}
