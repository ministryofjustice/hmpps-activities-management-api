package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSlot as EntityActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.TimeSlot as ModelTimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

class ActivityScheduleTest {

  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val tomorrow = today.plusDays(1)
  private val midnightSlot = TimeSlot.AM
  private val noonSlot = TimeSlot.PM

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
        onWing = false,
        offWing = false,
        pieceWork = false,
        outsideWork = false,
        payPerSession = PayPerSession.H,
        prisonCode = "MDI",
        summary = "Maths",
        description = "Maths basic",
        riskLevel = "high",
        minimumEducationLevel = listOf(
          ActivityMinimumEducationLevel(
            id = 0,
            educationLevelCode = "1",
            educationLevelDescription = "Reading Measure 1.0",
            studyAreaCode = "ENGLA",
            studyAreaDescription = "English Language",
          ),
        ),
        category = ModelActivityCategory(
          id = 1L,
          code = "category code",
          name = "category name",
          description = "category description",
        ),
        capacity = 1,
        allocated = 2,
        createdTime = today.atStartOfDay(),
        activityState = ActivityState.LIVE,
        paid = true,
      ),
      scheduleWeeks = 1,
      slots = listOf(
        ActivityScheduleSlot(
          id = 0,
          timeSlot = ModelTimeSlot.AM,
          weekNumber = 1,
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
      startDate = today,
      usePrisonRegimeTime = true,
    )
    assertThat(
      activitySchedule(
        activityEntity(),
        timestamp = today.atTime(10, 20),
        startDate = today,
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
          onWing = false,
          offWing = false,
          pieceWork = false,
          outsideWork = false,
          payPerSession = PayPerSession.H,
          prisonCode = "MDI",
          summary = "Maths",
          description = "Maths basic",
          riskLevel = "high",
          minimumEducationLevel = listOf(
            ActivityMinimumEducationLevel(
              id = 0,
              educationLevelCode = "1",
              educationLevelDescription = "Reading Measure 1.0",
              studyAreaCode = "ENGLA",
              studyAreaDescription = "English Language",
            ),
          ),
          category = ModelActivityCategory(
            id = 1L,
            code = "category code",
            name = "category name",
            description = "category description",
          ),
          capacity = 1,
          allocated = 2,
          createdTime = today.atStartOfDay(),
          activityState = ActivityState.LIVE,
          paid = true,
        ),
        scheduleWeeks = 1,
        slots = listOf(
          ActivityScheduleSlot(
            id = 0,
            timeSlot = ModelTimeSlot.AM,
            weekNumber = 1,
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
        startDate = today,
        usePrisonRegimeTime = true,
      ),
    )

    assertThat(
      listOf(
        activitySchedule(
          activityEntity(),
          timestamp = today.atTime(10, 20),
          startDate = today,
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
      startDate = today,
      prisonerNumber = "123456".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 10001,
      allocatedBy = "FRED",
    )

    assertThat(schedule.allocations()).hasSize(1)

    with(schedule.allocations().first()) {
      assertThat(activitySchedule).isEqualTo(schedule)
      assertThat(prisonerNumber).isEqualTo("123456")
      assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE)
      assertThat(payBand).isEqualTo(lowPayBand)
      assertThat(startDate).isEqualTo(today)
      assertThat(allocatedBy).isEqualTo("FRED")
      assertThat(allocatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    }
  }

  @Test
  fun `can allocate prisoner to a schedule with existing allocation`() {
    val schedule = activitySchedule(activity = activityEntity())
      .also { assertThat(it.allocations()).hasSize(2) }

    schedule.allocatePrisoner(
      startDate = today,
      prisonerNumber = "654321".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 10001,
      allocatedBy = "FREDDIE",
    )

    assertThat(schedule.allocations()).hasSize(3)

    with(schedule.allocations().first { it.prisonerNumber == "654321" }) {
      assertThat(activitySchedule).isEqualTo(schedule)
      assertThat(prisonerNumber).isEqualTo("654321")
      assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE)
      assertThat(bookingId).isEqualTo(10001)
      assertThat(payBand).isEqualTo(lowPayBand)
      assertThat(startDate).isEqualTo(today)
      assertThat(allocatedBy).isEqualTo("FREDDIE")
      assertThat(allocatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(payBand).isEqualTo(lowPayBand)
    }
  }

  @Test
  fun `can allocate prisoner to a schedule with an exclusion`() {
    val schedule = activitySchedule(activity = activityEntity())
      .also { it.allocations() hasSize 2 }

    schedule.allocatePrisoner(
      startDate = today,
      prisonerNumber = "654321".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 10001,
      exclusions = listOf(
        Slot(
          weekNumber = 1,
          timeSlot = ModelTimeSlot.AM,
          monday = true,
        ),
      ),
      allocatedBy = "FREDDIE",
    )

    schedule.allocations() hasSize 3

    with(schedule.allocations().first { it.prisonerNumber == "654321" }) {
      exclusions(ExclusionsFilter.ACTIVE) hasSize 1
      exclusions(ExclusionsFilter.ACTIVE).first().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY)
    }
  }

  @Test
  fun `can not allocate prisoner to a schedule with an exclusion which does not relate to a real slot`() {
    val schedule = activitySchedule(activity = activityEntity())
      .also { it.allocations() hasSize 2 }

    assertThatThrownBy {
      schedule.allocatePrisoner(
        startDate = today,
        prisonerNumber = "654321".toPrisonerNumber(),
        payBand = lowPayBand,
        bookingId = 10001,
        exclusions = listOf(
          Slot(
            weekNumber = 3,
            timeSlot = ModelTimeSlot.AM,
            monday = true,
          ),
        ),
        allocatedBy = "FREDDIE",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocating to schedule 1: No AM slots in week number 3")

    schedule.allocations() hasSize 2
  }

  @Test
  fun `allocated prisoner status is set to PENDING when start date is in the future`() {
    val schedule = activitySchedule(activity = activityEntity())
      .also { assertThat(it.allocations()).hasSize(2) }

    schedule.allocatePrisoner(
      prisonerNumber = "654321".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 10001,
      allocatedBy = "FREDDIE",
      startDate = tomorrow,
    )

    assertThat(schedule.allocations()).hasSize(3)

    with(schedule.allocations().first { it.prisonerNumber == "654321" }) {
      assertThat(activitySchedule).isEqualTo(schedule)
      assertThat(prisonerNumber).isEqualTo("654321")
      assertThat(prisonerStatus).isEqualTo(PrisonerStatus.PENDING)
      assertThat(bookingId).isEqualTo(10001)
      assertThat(payBand).isEqualTo(lowPayBand)
      assertThat(startDate).isEqualTo(tomorrow)
      assertThat(allocatedBy).isEqualTo("FREDDIE")
      assertThat(allocatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(payBand).isEqualTo(lowPayBand)
    }
  }

  @Test
  fun `cannot allocate prisoner if already allocated to schedule`() {
    val schedule = activitySchedule(activity = activityEntity(), noAllocations = true)

    schedule.allocatePrisoner(
      startDate = today,
      prisonerNumber = "654321".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 10001,
      allocatedBy = "FREDDIE",
    )

    assertThatThrownBy {
      schedule.allocatePrisoner(
        startDate = tomorrow,
        prisonerNumber = "654321".toPrisonerNumber(),
        payBand = lowPayBand,
        bookingId = 10001,
        allocatedBy = "NOT FREDDIE",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner '654321' is already allocated to schedule ${schedule.description}.")
  }

  @Test
  fun `cannot start allocation beyond the end date of the schedule or activity`() {
    val schedule =
      activitySchedule(activity = activityEntity(startDate = yesterday, endDate = today), noAllocations = true)

    assertThatThrownBy {
      schedule.allocatePrisoner(
        prisonerNumber = "654321".toPrisonerNumber(),
        payBand = lowPayBand,
        bookingId = 10001,
        allocatedBy = "NOT FREDDIE",
        startDate = tomorrow,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation start date cannot be after the activity end date.")
  }

  @Test
  fun `can re-allocate prisoner providing previous allocation is ended`() {
    val schedule = activitySchedule(activity = activityEntity(), noAllocations = true)

    val allocation = schedule.allocatePrisoner(
      startDate = tomorrow,
      prisonerNumber = "654321".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 10001,
      allocatedBy = "FREDDIE",
    )

    allocation.deallocateNowWithReason(DeallocationReason.OTHER)

    assertDoesNotThrow {
      schedule.allocatePrisoner(
        startDate = today,
        prisonerNumber = "654321".toPrisonerNumber(),
        payBand = lowPayBand,
        bookingId = 10001,
        allocatedBy = "NOT FREDDIE",
      )
    }
  }

  @Test
  fun `allocated by cannot be blank when allocating a prisoner`() {
    val schedule = activitySchedule(activity = activityEntity(), noAllocations = true)

    assertThatThrownBy {
      schedule.allocatePrisoner(
        startDate = today,
        prisonerNumber = "654321".toPrisonerNumber(),
        payBand = lowPayBand,
        bookingId = 10001,
        allocatedBy = " ",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocated by cannot be blank.")
  }

  @Test
  fun `can add one day slot only to schedule`() {
    val schedule = activitySchedule(activity = activityEntity(), noSlots = true)

    schedule.addSlot(1, LocalTime.MIDNIGHT to LocalTime.MIDNIGHT.plusHours(1), setOf(DayOfWeek.MONDAY), midnightSlot)

    assertThat(schedule.slots()).containsExactly(
      EntityActivityScheduleSlot(
        activitySchedule = schedule,
        weekNumber = 1,
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT.plusHours(1),
        mondayFlag = true,
        timeSlot = midnightSlot,
      ),
    )
  }

  @Test
  fun `can add two day slot to schedule`() {
    val schedule = activitySchedule(activity = activityEntity(), noSlots = true)

    schedule.addSlot(
      1,
      LocalTime.MIDNIGHT to LocalTime.MIDNIGHT.plusHours(1),
      setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
      midnightSlot,
    )

    assertThat(schedule.slots()).containsExactly(
      EntityActivityScheduleSlot(
        activitySchedule = schedule,
        weekNumber = 1,
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT.plusHours(1),
        mondayFlag = true,
        wednesdayFlag = true,
        timeSlot = midnightSlot,
      ),
    )
  }

  @Test
  fun `can add entire week slot to schedule`() {
    val schedule = activitySchedule(activity = activityEntity(), noSlots = true)

    schedule.addSlot(1, LocalTime.MIDNIGHT to LocalTime.MIDNIGHT.plusHours(1), DayOfWeek.entries.toSet(), midnightSlot)

    assertThat(schedule.slots()).containsExactly(
      EntityActivityScheduleSlot(
        activitySchedule = schedule,
        weekNumber = 1,
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT.plusHours(1),
        mondayFlag = true,
        tuesdayFlag = true,
        wednesdayFlag = true,
        thursdayFlag = true,
        fridayFlag = true,
        saturdayFlag = true,
        sundayFlag = true,
        timeSlot = midnightSlot,
      ),
    )
  }

  @Test
  fun `fails to add slot when a slot already exists with the same week and timeSlot`() {
    val schedule = activityEntity().schedules().first()

    schedule.slots().single().weekNumber isEqualTo 1
    schedule.slots().single().slotTimes() isEqualTo (LocalTime.MIDNIGHT to LocalTime.MIDNIGHT.plusHours(1))

    assertThatThrownBy {
      schedule.addSlot(1, LocalTime.MIDNIGHT to LocalTime.MIDNIGHT.plusHours(1), setOf(DayOfWeek.MONDAY), midnightSlot)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Adding slot to activity schedule with ID 1: Slot already exists from 00:00 to 01:00 for week number 1")
  }

  @Test
  fun `fails to add slot when end time not after start time`() {
    val schedule = activityEntity().schedules().first()

    assertThatThrownBy {
      schedule.addSlot(1, LocalTime.NOON to LocalTime.NOON, setOf(DayOfWeek.MONDAY), midnightSlot)
    }.isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun `can add slots for multi-week schedule`() {
    val activity = activityEntity(noSchedules = true)
    val schedule = activitySchedule(activity, noSlots = true, scheduleWeeks = 2)

    schedule.addSlot(
      weekNumber = 1,
      slotTimes = LocalTime.NOON to LocalTime.NOON.plusHours(1),
      daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
      timeSlot = noonSlot,
    )

    schedule.addSlot(
      weekNumber = 2,
      slotTimes = LocalTime.NOON to LocalTime.NOON.plusHours(1),
      daysOfWeek = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
      timeSlot = noonSlot,
    )

    assertThat(schedule.slots()).containsExactly(
      EntityActivityScheduleSlot(
        activitySchedule = schedule,
        weekNumber = 1,
        startTime = LocalTime.NOON,
        endTime = LocalTime.NOON.plusHours(1),
        mondayFlag = true,
        fridayFlag = true,
        timeSlot = noonSlot,
      ),
      EntityActivityScheduleSlot(
        activitySchedule = schedule,
        weekNumber = 2,
        startTime = LocalTime.NOON,
        endTime = LocalTime.NOON.plusHours(1),
        wednesdayFlag = true,
        thursdayFlag = true,
        timeSlot = noonSlot,
      ),
    )
  }

  @Test
  fun `end date must be on or after the start date`() {
    val schedule = activityEntity().schedules().first().apply { endDate = null }

    assertThat(schedule.endDate).isNull()

    schedule.endDate = schedule.startDate.plusDays(1)

    assertThatThrownBy {
      schedule.endDate = schedule.startDate.minusDays(1)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity schedule end date cannot be before activity schedule start date.")
  }

  @Test
  fun `fails if no day specified for a slot`() {
    val schedule = activityEntity().schedules().first()

    assertThatThrownBy {
      schedule.addSlot(1, LocalTime.NOON to LocalTime.NOON.plusHours(1), emptySet(), noonSlot)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("A slot must run on at least one day.")
  }

  @Test
  fun `fails to initialise if capacity not greater than zero`() {
    ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = today,
      scheduleWeeks = 1,
    )

    assertThatThrownBy {
      ActivitySchedule(
        activity = activityEntity(),
        description = "description",
        capacity = 0,
        startDate = today,
        scheduleWeeks = 1,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The schedule capacity must be greater than zero.")
  }

  @Test
  fun `fails to initialise if schedule weeks not greater than zero`() {
    assertThatThrownBy {
      ActivitySchedule(
        activity = activityEntity(),
        description = "description",
        capacity = 1,
        startDate = today,
        scheduleWeeks = 0,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Schedule weeks must be greater than zero.")

    assertThatThrownBy {
      ActivitySchedule(
        activity = activityEntity(),
        description = "description",
        capacity = 1,
        startDate = today,
        scheduleWeeks = -1,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Schedule weeks must be greater than zero.")
  }

  @Test
  fun `gets schedule week number for given date (1 week schedule)`() {
    val activitySchedule = ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = LocalDate.parse("2023-07-14"),
      scheduleWeeks = 1,
    )

    listOf(
      Pair(LocalDate.parse("2023-07-14"), 1),
      Pair(LocalDate.parse("2023-07-21"), 1),
      Pair(LocalDate.parse("2023-08-01"), 1),
      Pair(LocalDate.parse("2023-12-15"), 1),
      Pair(LocalDate.parse("2024-01-01"), 1),
      Pair(LocalDate.parse("2024-01-15"), 1),
      Pair(LocalDate.parse("2028-08-15"), 1),
    ).forEach {
      assertThat(activitySchedule.getWeekNumber(it.first)).isEqualTo(it.second)
    }
  }

  @Test
  fun `gets schedule week number for given date (2 week schedule)`() {
    val activitySchedule = ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = LocalDate.parse("2023-07-14"),
      scheduleWeeks = 2,
    )

    listOf(
      Pair(LocalDate.parse("2023-07-14"), 1),
      Pair(LocalDate.parse("2023-07-21"), 2),
      Pair(LocalDate.parse("2023-08-01"), 2),
      Pair(LocalDate.parse("2023-12-15"), 1),
      Pair(LocalDate.parse("2024-01-01"), 2),
      Pair(LocalDate.parse("2024-01-15"), 2),
      Pair(LocalDate.parse("2028-08-15"), 1),
    ).forEach {
      assertThat(activitySchedule.getWeekNumber(it.first)).isEqualTo(it.second)
    }
  }

  @Test
  fun `getWeekNumber() throws error when date comes before activity start date`() {
    assertThatThrownBy {
      ActivitySchedule(
        activity = activityEntity(),
        description = "description",
        capacity = 1,
        startDate = LocalDate.parse("2023-07-13"),
        scheduleWeeks = 1,
      ).getWeekNumber(LocalDate.parse("2023-07-12"))
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Date must be within activity schedule range.")
  }

  @Test
  fun `getWeekNumber() throws error when date comes after activity end date`() {
    assertThatThrownBy {
      ActivitySchedule(
        activity = activityEntity(
          startDate = LocalDate.parse("2023-07-13"),
          endDate = LocalDate.parse("2023-08-13"),
        ),
        description = "description",
        capacity = 1,
        startDate = LocalDate.parse("2023-07-13"),
        scheduleWeeks = 1,
      ).apply {
        endDate = LocalDate.parse("2023-08-13")
      }.getWeekNumber(LocalDate.parse("2023-08-14"))
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Date must be within activity schedule range.")
  }

  @Test
  fun `check activity active status that starts today with open end date`() {
    val scheduleWithNoEndDate = ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = today,
      scheduleWeeks = 1,
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
      scheduleWeeks = 1,
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
  fun `check an activity that starts tomorrow is in the future`() {
    val scheduleWithEndDate = ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = tomorrow,
      scheduleWeeks = 1,
    ).apply {
      endDate = tomorrow.plusDays(3)
    }

    with(scheduleWithEndDate) {
      assertThat(isFuture(today)).isTrue
      assertThat(isFuture(tomorrow)).isFalse
    }
  }

  @Test
  fun `can add scheduled instance to a schedule`() {
    val scheduleWithSlot = ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = today,
      scheduleWeeks = 1,
    ).apply {
      addSlot(
        weekNumber = 1,
        slotTimes = today.atStartOfDay().toLocalTime() to today.atStartOfDay().toLocalTime().plusHours(1),
        setOf(DayOfWeek.MONDAY),
        timeSlot = TimeSlot.AM,
      )
    }

    assertThat(scheduleWithSlot.instances()).isEmpty()
    scheduleWithSlot.addInstance(today, scheduleWithSlot.slots().first())
    assertThat(scheduleWithSlot.instances()).hasSize(1)
  }

  @Test
  fun `adding instance to schedule updates instancesLastUpdatedTime`() {
    val activity = activityEntity()
    val schedule = activity.schedules().first().apply {
      instancesLastUpdatedTime = null
    }

    assertThat(schedule.instancesLastUpdatedTime).isNull()

    schedule.addInstance(tomorrow, schedule.slots().first())

    assertThat(schedule.instancesLastUpdatedTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
  }

  @Test
  fun `remove instance from schedule updates instancesLastUpdatedTime`() {
    val activity = activityEntity()
    val schedule = activity.schedules().first().apply {
      instancesLastUpdatedTime = null
    }

    assertThat(schedule.instancesLastUpdatedTime).isNull()

    schedule.removeInstances(listOf(schedule.instances().first()))

    assertThat(schedule.instancesLastUpdatedTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
  }

  @Test
  fun `cannot add duplicate scheduled instance to a schedule`() {
    val scheduleWithSlot = ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = today,
      scheduleWeeks = 1,
    ).apply {
      addSlot(
        weekNumber = 1,
        slotTimes = today.atStartOfDay().toLocalTime() to today.atStartOfDay().toLocalTime().plusHours(1),
        setOf(DayOfWeek.MONDAY),
        timeSlot = TimeSlot.AM,
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
      scheduleWeeks = 1,
    ).apply {
      addSlot(
        weekNumber = 1,
        slotTimes = today.atStartOfDay().toLocalTime() to today.atStartOfDay().toLocalTime().plusHours(1),
        setOf(DayOfWeek.MONDAY),
        timeSlot = TimeSlot.AM,
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

  @Test
  fun `can retrieve previous and next instances where available`() {
    val scheduleWithInstances = ActivitySchedule(
      activity = activityEntity(),
      description = "description",
      capacity = 1,
      startDate = today,
      internalLocationId = 1,
      internalLocationCode = "Loc Code",
      internalLocationDescription = "Loc Code Desc",
      scheduleWeeks = 1,
    ).apply {
      addSlot(
        weekNumber = 1,
        slotTimes = today.atStartOfDay().toLocalTime() to today.atStartOfDay().toLocalTime().plusHours(1),
        setOf(DayOfWeek.MONDAY),
        timeSlot = TimeSlot.AM,
      )
    }

    // Adding out of order is intentional for verifying previous and next functionality
    val secondInstance = scheduleWithInstances.addInstance(today.plusDays(1L), scheduleWithInstances.slots().first())
    val fourthInstance = scheduleWithInstances.addInstance(today.plusDays(3L), scheduleWithInstances.slots().first())
    val thirdInstance = scheduleWithInstances.addInstance(today.plusDays(2L), scheduleWithInstances.slots().first())
    val firstInstance = scheduleWithInstances.addInstance(today, scheduleWithInstances.slots().first())

    assertThat(scheduleWithInstances.instances()).hasSize(4)

    with(scheduleWithInstances) {
      assertThat(previous(firstInstance)).isNull()
      assertThat(next(firstInstance)).isEqualTo(secondInstance)

      assertThat(previous(secondInstance)).isEqualTo(firstInstance)
      assertThat(next(secondInstance)).isEqualTo(thirdInstance)

      assertThat(previous(thirdInstance)).isEqualTo(secondInstance)
      assertThat(next(thirdInstance)).isEqualTo(fourthInstance)

      assertThat(previous(fourthInstance)).isEqualTo(thirdInstance)
      assertThat(next(fourthInstance)).isNull()
    }
  }

  @Test
  fun `in-cell activity should not contain internal location`() {
    val activitySchedule = activitySchedule(
      activityEntity(
        inCell = true,
      ),
    ).toModelLite()

    assertThat(activitySchedule.id).isEqualTo(1)
    assertThat(activitySchedule.internalLocation).isNull()

    with(activitySchedule.activity) {
      assertThat(id).isEqualTo(1)
      assertThat(inCell).isTrue
      assertThat(onWing).isFalse
    }
  }

  @Test
  fun `on-wing activity should not contain internal location`() {
    val activitySchedule = activitySchedule(
      activityEntity(
        onWing = true,
      ),
    ).toModelLite()

    assertThat(activitySchedule.id).isEqualTo(1)
    assertThat(activitySchedule.internalLocation).isNull()

    with(activitySchedule.activity) {
      assertThat(id).isEqualTo(1)
      assertThat(inCell).isFalse
      assertThat(onWing).isTrue
    }
  }

  @Test
  fun `prisoner is deallocated from schedule`() {
    val schedule = activitySchedule(activity = activityEntity())
    val allocation =
      schedule.allocations().first().also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    assertThat(allocation.plannedDeallocation).isNull()

    schedule.deallocatePrisonerOn(
      allocation.prisonerNumber,
      TimeSource.tomorrow(),
      DeallocationReason.RELEASED,
      "by test",
    )

    with(allocation.plannedDeallocation!!) {
      assertThat(plannedDate).isEqualTo(TimeSource.tomorrow())
      assertThat(plannedBy).isEqualTo("by test")
      assertThat(plannedReason).isEqualTo(DeallocationReason.RELEASED)
    }
  }

  @Test
  fun `prisoner is deallocated from schedule when they already have an ended allocation previously`() {
    val schedule = activitySchedule(activity = activityEntity())
    val originalAllocation = schedule.allocations().first().also { it.deallocateNowOn(TimeSource.today()) }
    val newAllocation = schedule.allocatePrisoner(originalAllocation.prisonerNumber.toPrisonerNumber(), originalAllocation.payBand, originalAllocation.bookingId, today, allocatedBy = "test")

    assertThat(newAllocation.plannedDeallocation).isNull()

    schedule.deallocatePrisonerOn(
      newAllocation.prisonerNumber,
      TimeSource.tomorrow(),
      DeallocationReason.RELEASED,
      "by test",
    )

    with(newAllocation.plannedDeallocation!!) {
      assertThat(plannedDate).isEqualTo(TimeSource.tomorrow())
      assertThat(plannedBy).isEqualTo("by test")
      assertThat(plannedReason).isEqualTo(DeallocationReason.RELEASED)
    }
  }

  @Test
  fun `prisoner is deallocated from schedule when the schedule is yet to start`() {
    val schedule = activitySchedule(activity = activityEntity(startDate = tomorrow))
    val originalAllocation = schedule.allocations().first()

    assertThat(originalAllocation.plannedDeallocation).isNull()

    val newAllocation = schedule.deallocatePrisonerOn(
      originalAllocation.prisonerNumber,
      today,
      DeallocationReason.WITHDRAWN_STAFF,
      "by test",
    )

    with(newAllocation) {
      assertThat(allocationId).isEqualTo(originalAllocation.allocationId)
      assertThat(deallocatedBy).isEqualTo("by test")
      assertThat(deallocatedReason).isEqualTo(DeallocationReason.WITHDRAWN_STAFF)
      assertThat(deallocatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(endDate).isEqualTo(startDate)
    }
  }

  @Test
  fun `prisoner is not deallocated from inactive schedule`() {
    val schedule = activitySchedule(activity = activityEntity(startDate = yesterday.minusDays(1), endDate = yesterday))

    val allocation =
      schedule.allocations().first().also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    assertThatThrownBy {
      schedule.deallocatePrisonerOn(
        allocation.prisonerNumber,
        TimeSource.tomorrow(),
        DeallocationReason.RELEASED,
        "by test",
      )
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Schedule ${schedule.activityScheduleId} is not active or in the future on the planned deallocated date ${TimeSource.tomorrow()}.")
  }

  @Test
  fun `fails to deallocate when prisoner not allocated to schedule`() {
    val schedule = activitySchedule(activity = activityEntity())

    assertThatThrownBy {
      schedule.deallocatePrisonerOn("DOES NOT EXIST", TimeSource.tomorrow(), DeallocationReason.RELEASED, "by test")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation not found for prisoner DOES NOT EXIST for schedule ${schedule.activityScheduleId}.")
  }

  @Test
  fun `schedule has not instances on a given date`() {
    val schedule =
      activitySchedule(activity = activityEntity(startDate = yesterday), noSlots = true, noInstances = true)

    assertThat(schedule.slots()).isEmpty()

    val slotOne = schedule.addSlot(1, LocalTime.MIDNIGHT to LocalTime.NOON, setOf(today.dayOfWeek), midnightSlot)
    val slotTwo = schedule.addSlot(1, LocalTime.NOON to LocalTime.NOON.plusHours(1), setOf(tomorrow.dayOfWeek), noonSlot)

    schedule.addInstance(today, slotOne)
    schedule.addInstance(tomorrow, slotTwo)

    with(schedule) {
      assertThat(hasNoInstancesOnDate(yesterday, slotOne.startTime to slotOne.endTime)).isTrue
      assertThat(hasNoInstancesOnDate(today, slotOne.startTime to slotOne.endTime)).isFalse
      assertThat(hasNoInstancesOnDate(tomorrow, slotTwo.startTime to slotTwo.endTime)).isFalse
      assertThat(hasNoInstancesOnDate(today, slotTwo.startTime to slotTwo.endTime)).isTrue
      assertThat(hasNoInstancesOnDate(tomorrow, slotOne.startTime to slotOne.endTime)).isTrue
    }
  }

  @Test
  fun `change to schedule end date is applied to allocations`() {
    val schedule =
      activitySchedule(activity = activityEntity(), noAllocations = true, startDate = yesterday, endDate = tomorrow)

    schedule.allocatePrisoner(
      startDate = today,
      prisonerNumber = "1111111".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 10001,
      allocatedBy = "FRED",
      endDate = tomorrow,
    )

    schedule.allocatePrisoner(
      startDate = today,
      prisonerNumber = "2222222".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 20002,
      allocatedBy = "BILL",
      endDate = tomorrow,
    )

    assertThat(schedule.allocations()).hasSize(2)

    schedule.allocations().forEach { allocation -> assertThat(allocation.endDate).isEqualTo(tomorrow) }

    schedule.endDate = today

    schedule.allocations().forEach { allocation -> assertThat(allocation.endDate).isEqualTo(today) }
  }

  @Test
  fun `change to schedule end date is is not applied to ended allocations`() {
    val schedule =
      activitySchedule(
        activity = activityEntity(),
        noAllocations = true,
        startDate = yesterday,
        endDate = today.plusWeeks(1),
      )

    val activeAllocation = schedule.allocatePrisoner(
      startDate = today,
      prisonerNumber = "1111111".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 10001,
      allocatedBy = "FRED",
      endDate = tomorrow,
    )

    val endedAllocation = schedule.allocatePrisoner(
      startDate = today,
      prisonerNumber = "2222222".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 20002,
      allocatedBy = "BILL",
      endDate = tomorrow.plusDays(1),
    ).deallocateNowOn(TimeSource.today())

    assertThat(schedule.allocations()).hasSize(2)

    schedule.endDate = tomorrow

    assertThat(activeAllocation.endDate).isEqualTo(tomorrow)
    assertThat(endedAllocation.endDate).isEqualTo(today)
  }

  @Test
  fun `check schedule ends`() {
    with(activitySchedule(activityEntity(startDate = yesterday, endDate = yesterday))) {
      endsOn(yesterday) isBool true
      endsOn(today) isBool false
      endsOn(tomorrow) isBool false
    }

    with(activitySchedule(activityEntity(endDate = today))) {
      endsOn(yesterday) isBool false
      endsOn(today) isBool true
      endsOn(tomorrow) isBool false
    }

    with(activitySchedule(activityEntity(endDate = tomorrow))) {
      endsOn(yesterday) isBool false
      endsOn(today) isBool false
      endsOn(tomorrow) isBool true
    }

    with(activitySchedule(activityEntity(endDate = null))) {
      endsOn(yesterday) isBool false
      endsOn(today) isBool false
      endsOn(tomorrow) isBool false
    }
  }
}
