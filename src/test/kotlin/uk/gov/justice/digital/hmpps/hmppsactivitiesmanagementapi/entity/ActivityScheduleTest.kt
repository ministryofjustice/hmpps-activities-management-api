package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSlot as EntityActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

class ActivityScheduleTest {

  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val tomorrow = today.plusDays(1)

  @Test
  fun `converted to model lite`() {
    val expectedModel = ActivityScheduleLite(
      id = 1,
      description = "schedule description",
      internalLocation = InternalLocation(1, "EDU-ROOM-1", "Education - R1"),
      capacity = 1,
      activity = ActivityLite(
        id = 1L,
        attendanceRequired = true,
        inCell = false,
        pieceWork = false,
        outsideWork = false,
        payPerSession = PayPerSession.H,
        prisonCode = "123",
        summary = "Maths",
        description = "Maths basic",
        riskLevel = "High",
        minimumIncentiveNomisCode = "BAS",
        minimumIncentiveLevel = "Basic",
        minimumEducationLevel = listOf(
          ActivityMinimumEducationLevel(
            id = -1,
            educationLevelCode = "1",
            educationLevelDescription = "Reading Measure 1.0",
          ),
        ),
        category = ModelActivityCategory(
          id = 1L,
          code = "category code",
          name = "category name",
          description = "category description",
        ),
      ),
      slots = listOf(
        ActivityScheduleSlot(
          id = 1L,
          startTime = LocalTime.of(10, 20),
          endTime = LocalTime.of(11, 20),
          daysOfWeek = listOf("Mon"),
          mondayFlag = true,
          tuesdayFlag = false,
          wednesdayFlag = false,
          thursdayFlag = false,
          fridayFlag = false,
          saturdayFlag = false,
          sundayFlag = false,
        ),
      ),
      startDate = LocalDate.now().plusDays(1),
    )
    assertThat(
      activitySchedule(
        activityEntity(),
        timestamp = LocalDate.now().atTime(10, 20),
        startDate = LocalDate.now().plusDays(1),
      ).toModelLite(),
    ).isEqualTo(expectedModel)
  }

  @Test
  fun `List converted to model lite`() {
    val expectedModel = listOf(
      ActivityScheduleLite(
        id = 1,
        description = "schedule description",
        internalLocation = InternalLocation(1, "EDU-ROOM-1", "Education - R1"),
        capacity = 1,
        activity = ActivityLite(
          id = 1L,
          attendanceRequired = true,
          inCell = false,
          pieceWork = false,
          outsideWork = false,
          payPerSession = PayPerSession.H,
          prisonCode = "123",
          summary = "Maths",
          description = "Maths basic",
          riskLevel = "High",
          minimumIncentiveNomisCode = "BAS",
          minimumIncentiveLevel = "Basic",
          minimumEducationLevel = listOf(
            ActivityMinimumEducationLevel(
              id = -1,
              educationLevelCode = "1",
              educationLevelDescription = "Reading Measure 1.0",
            ),
          ),
          category = ModelActivityCategory(
            id = 1L,
            code = "category code",
            name = "category name",
            description = "category description",
          ),
        ),
        slots = listOf(
          ActivityScheduleSlot(
            id = 1L,
            startTime = LocalTime.of(10, 20),
            endTime = LocalTime.of(11, 20),
            daysOfWeek = listOf("Mon"),
            mondayFlag = true,
            tuesdayFlag = false,
            wednesdayFlag = false,
            thursdayFlag = false,
            fridayFlag = false,
            saturdayFlag = false,
            sundayFlag = false,
          ),
        ),
        startDate = LocalDate.now().plusDays(1),
      ),
    )

    assertThat(
      listOf(
        activitySchedule(
          activityEntity(),
          timestamp = LocalDate.now().atTime(10, 20),
          startDate = LocalDate.now().plusDays(1),
        ),
      ).toModelLite(),
    ).isEqualTo(
      expectedModel,
    )
  }

  @Test
  fun `can allocate prisoner to a schedule with no allocations`() {
    val schedule = activitySchedule(activity = activityEntity(), noAllocations = true)

    schedule.allocatePrisoner(
      prisonerNumber = "123456".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 10001,
      allocatedBy = "FRED",
    )

    assertThat(schedule.allocations()).hasSize(1)

    with(schedule.allocations().first()) {
      assertThat(activitySchedule).isEqualTo(schedule)
      assertThat(prisonerNumber).isEqualTo("123456")
      assertThat(payBand).isEqualTo(lowPayBand)
      assertThat(startDate).isEqualTo(LocalDate.now())
      assertThat(allocatedBy).isEqualTo("FRED")
      assertThat(allocatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    }
  }

  @Test
  fun `can allocate prisoner to a schedule with existing allocation`() {
    val schedule = activitySchedule(activity = activityEntity())
      .also { assertThat(it.allocations()).hasSize(1) }

    schedule.allocatePrisoner(
      prisonerNumber = "654321".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 10001,
      allocatedBy = "FREDDIE",
    )

    assertThat(schedule.allocations()).hasSize(2)

    with(schedule.allocations().first { it.prisonerNumber == "654321" }) {
      assertThat(activitySchedule).isEqualTo(schedule)
      assertThat(prisonerNumber).isEqualTo("654321")
      assertThat(bookingId).isEqualTo(10001)
      assertThat(payBand).isEqualTo(lowPayBand)
      assertThat(startDate).isEqualTo(LocalDate.now())
      assertThat(allocatedBy).isEqualTo("FREDDIE")
      assertThat(allocatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(payBand).isEqualTo(lowPayBand)
    }
  }

  @Test
  fun `cannot allocate prisoner if already allocated to schedule`() {
    val schedule = activitySchedule(activity = activityEntity(), noAllocations = true)

    schedule.allocatePrisoner(
      prisonerNumber = "654321".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 10001,
      allocatedBy = "FREDDIE",
    )

    assertThatThrownBy {
      schedule.allocatePrisoner(
        prisonerNumber = "654321".toPrisonerNumber(),
        payBand = lowPayBand,
        bookingId = 10001,
        allocatedBy = "NOT FREDDIE",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun `allocated by cannot be blank when allocating a prisoner`() {
    val schedule = activitySchedule(activity = activityEntity(), noAllocations = true)

    assertThatThrownBy {
      schedule.allocatePrisoner(
        prisonerNumber = "654321".toPrisonerNumber(),
        payBand = lowPayBand,
        bookingId = 10001,
        allocatedBy = " ",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun `can add one day slot only to schedule`() {
    val schedule = activitySchedule(activity = activityEntity(), noSlots = true)

    schedule.addSlot(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1), setOf(DayOfWeek.MONDAY))

    assertThat(schedule.slots()).containsExactly(
      EntityActivityScheduleSlot(
        activitySchedule = schedule,
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT.plusHours(1),
        mondayFlag = true,
      ),
    )
  }

  @Test
  fun `can add two day slot to schedule`() {
    val schedule = activitySchedule(activity = activityEntity(), noSlots = true)

    schedule.addSlot(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1), setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))

    assertThat(schedule.slots()).containsExactly(
      EntityActivityScheduleSlot(
        activitySchedule = schedule,
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT.plusHours(1),
        mondayFlag = true,
        wednesdayFlag = true,
      ),
    )
  }

  @Test
  fun `can add entire week slot to schedule`() {
    val schedule = activitySchedule(activity = activityEntity(), noSlots = true)

    schedule.addSlot(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1), DayOfWeek.values().toSet())

    assertThat(schedule.slots()).containsExactly(
      EntityActivityScheduleSlot(
        activitySchedule = schedule,
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT.plusHours(1),
        mondayFlag = true,
        tuesdayFlag = true,
        wednesdayFlag = true,
        thursdayFlag = true,
        fridayFlag = true,
        saturdayFlag = true,
        sundayFlag = true,
      ),
    )
  }

  @Test
  fun `cannot add slot belonging to another schedule`() {
    val schedule = activitySchedule(activity = activityEntity(), noSlots = true)

    schedule.addSlot(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1), DayOfWeek.values().toSet())

    val expected = EntityActivityScheduleSlot(
      activitySchedule = schedule,
      startTime = LocalTime.MIDNIGHT,
      endTime = LocalTime.MIDNIGHT.plusHours(1),
      mondayFlag = true,
      tuesdayFlag = true,
      wednesdayFlag = true,
      thursdayFlag = true,
      fridayFlag = true,
      saturdayFlag = true,
      sundayFlag = true,
    )

    val slotBelongingToAnotherDifferentSchedule =
      expected.copy(activitySchedule = schedule.copy(activityScheduleId = -99))

    assertThatThrownBy {
      schedule.addSlot(slotBelongingToAnotherDifferentSchedule)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Can only add slots that belong to this schedule.")

    assertThat(schedule.slots()).containsExactly(expected)
  }

  @Test
  fun `fails to add slot when end time not after start time`() {
    val schedule = activityEntity().schedules().first()

    assertThatThrownBy {
      schedule.addSlot(LocalTime.NOON, LocalTime.NOON, setOf(DayOfWeek.MONDAY))
    }.isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun `end date must be after the start date`() {
    val schedule = activityEntity().schedules().first().apply { endDate = null }

    assertThat(schedule.endDate).isNull()

    schedule.endDate = schedule.startDate.plusDays(1)

    assertThatThrownBy { schedule.endDate = schedule.startDate }.isInstanceOf(IllegalArgumentException::class.java)
    assertThatThrownBy {
      schedule.endDate = schedule.startDate.minusDays(1)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("End date must be after the start date")
  }

  @Test
  fun `fails if no day specified for a slot`() {
    val schedule = activityEntity().schedules().first()

    assertThatThrownBy {
      schedule.addSlot(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1), emptySet())
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("One or more days must be specified for a given slot.")
  }

  @Test
  fun `fails to initialise if capacity not greater than zero`() {
    ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = LocalDate.now(),
    )

    assertThatThrownBy {
      ActivitySchedule(
        activity = activityEntity(),
        description = "description",
        capacity = 0,
        startDate = LocalDate.now(),
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The schedule capacity must be greater than zero.")
  }

  @Test
  fun `check activity active status that starts today with open end date`() {
    val scheduleWithNoEndDate = ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = today,
    )

    with(scheduleWithNoEndDate) {
      assertThat(isActiveOn(yesterday)).isFalse
      assertThat(isActiveOn(today)).isTrue
      assertThat(isActiveOn(tomorrow)).isTrue
      assertThat(isActiveOn(tomorrow.plusDays(1000))).isTrue
    }
  }

  @Test
  fun `check activity active status that starts today and ends tomorrow`() {
    val scheduleWithEndDate = ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = today,
    ).apply {
      endDate = tomorrow
    }

    with(scheduleWithEndDate) {
      assertThat(isActiveOn(yesterday)).isFalse
      assertThat(isActiveOn(today)).isTrue
      assertThat(isActiveOn(tomorrow)).isTrue
      assertThat(isActiveOn(tomorrow.plusDays(1))).isFalse
    }
  }

  @Test
  fun `can add scheduled instance to a schedule`() {
    val scheduleWithSlot = ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = today,
    ).apply {
      addSlot(
        startTime = today.atStartOfDay().toLocalTime(),
        endTime = today.atStartOfDay().toLocalTime().plusHours(1),
        setOf(DayOfWeek.MONDAY),
      )
    }

    assertThat(scheduleWithSlot.instances()).isEmpty()

    scheduleWithSlot.addInstance(today, scheduleWithSlot.slots().first())

    assertThat(scheduleWithSlot.instances()).hasSize(1)
  }

  @Test
  fun `cannot add duplicate scheduled instance to a schedule`() {
    val scheduleWithSlot = ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = today,
    ).apply {
      addSlot(
        startTime = today.atStartOfDay().toLocalTime(),
        endTime = today.atStartOfDay().toLocalTime().plusHours(1),
        setOf(DayOfWeek.MONDAY),
      )
    }

    val slot = scheduleWithSlot.slots().first()

    scheduleWithSlot.addInstance(today, slot)

    assertThatThrownBy {
      scheduleWithSlot.addInstance(today, slot)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("An instance for date '$today', start time '${slot.startTime}' and end time '${slot.endTime}' already exists")

    assertThat(scheduleWithSlot.instances()).hasSize(1)
  }

  @Test
  fun `cannot add scheduled instance to a schedule if slot part of schedule already`() {
    val scheduleWithSlot = ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = today,
    ).apply {
      addSlot(
        startTime = today.atStartOfDay().toLocalTime(),
        endTime = today.atStartOfDay().toLocalTime().plusHours(1),
        setOf(DayOfWeek.MONDAY),
      )
    }

    val slot = scheduleWithSlot.slots().first().copy(activityScheduleSlotId = 2)

    assertThatThrownBy {
      scheduleWithSlot.addInstance(today, slot)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add instance for slot '${slot.activityScheduleSlotId}', slot does not belong to this schedule.")

    assertThat(scheduleWithSlot.instances()).isEmpty()
  }
}
