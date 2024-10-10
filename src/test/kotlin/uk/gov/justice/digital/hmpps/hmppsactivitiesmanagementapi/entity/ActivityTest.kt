package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eligibilityRuleFemale
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eligibilityRuleOver21
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.mediumPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.notInWorkCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel as ModelActivityMinimumEducationLevel

class ActivityTest {
  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val tomorrow = today.plusDays(1)

  private val activityWithNoEndDate = activityEntity(startDate = today, endDate = null)

  private val activityWithEndDate = activityWithNoEndDate.copy().apply { endDate = tomorrow }

  @Test
  fun `check activity active status that starts today with open end date`() {
    with(activityWithNoEndDate) {
      assertThat(isActive(yesterday)).isFalse
      assertThat(isActive(today)).isTrue
      assertThat(isActive(tomorrow)).isTrue
      assertThat(isActive(tomorrow.plusDays(1000))).isTrue
      assertThat(ends(yesterday)).isFalse
      assertThat(ends(today)).isFalse
      assertThat(ends(tomorrow)).isFalse
    }
  }

  @Test
  fun `check activity active status that starts today and ends tomorrow`() {
    with(activityWithEndDate) {
      assertThat(isActive(yesterday)).isFalse
      assertThat(isActive(today)).isTrue
      assertThat(isActive(tomorrow)).isTrue
      assertThat(isActive(tomorrow.plusDays(1))).isFalse
      assertThat(ends(yesterday)).isFalse
      assertThat(ends(today)).isFalse
      assertThat(ends(tomorrow)).isTrue
    }
  }

  @Test
  fun `converted to model lite with allocation details`() {
    val expectedModel = ActivityLite(
      id = 1,
      attendanceRequired = false,
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
      category = ActivityCategory(
        id = 1L,
        code = "category code",
        name = "category name",
        description = "category description",
      ),
      capacity = 1,
      allocated = 2,
      createdTime = LocalDate.now().atStartOfDay(),
      activityState = ActivityState.LIVE,
      paid = true,
      minimumEducationLevel = listOf(
        ModelActivityMinimumEducationLevel(
          id = 0,
          educationLevelCode = "1",
          educationLevelDescription = "Reading Measure 1.0",
          studyAreaCode = "ENGLA",
          studyAreaDescription = "English Language",
        ),
      ),
    )
    assertThat(activityEntity(attendanceRequired = false).toModelLite()).isEqualTo(expectedModel)
  }

  @Test
  fun `converted to model lite without allocation details`() {
    val expectedModel = ActivityLite(
      id = 1,
      attendanceRequired = false,
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
      category = ActivityCategory(
        id = 1L,
        code = "category code",
        name = "category name",
        description = "category description",
      ),
      capacity = 1,
      allocated = 0,
      createdTime = LocalDate.now().atStartOfDay(),
      activityState = ActivityState.LIVE,
      paid = true,
      minimumEducationLevel = listOf(
        ModelActivityMinimumEducationLevel(
          id = 0,
          educationLevelCode = "1",
          educationLevelDescription = "Reading Measure 1.0",
          studyAreaCode = "ENGLA",
          studyAreaDescription = "English Language",
        ),
      ),
    )
    assertThat(activityEntity(attendanceRequired = false).toModelLite(includeAllocations = false)).isEqualTo(expectedModel)
  }

  @Test
  fun `list converted to model lite`() {
    val expectedModel = listOf(
      ActivityLite(
        id = 1,
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
          ModelActivityMinimumEducationLevel(
            id = 0,
            educationLevelCode = "1",
            educationLevelDescription = "Reading Measure 1.0",
            studyAreaCode = "ENGLA",
            studyAreaDescription = "English Language",
          ),
        ),
        category = ActivityCategory(
          id = 1L,
          code = "category code",
          name = "category name",
          description = "category description",
        ),
        capacity = 1,
        allocated = 2,
        createdTime = LocalDate.now().atStartOfDay(),
        activityState = ActivityState.LIVE,
        paid = true,
      ),
    )

    assertThat(listOf(activityEntity()).toModelLite()).isEqualTo(expectedModel)
  }

  @Test
  fun `can add schedule to activity`() {
    val activity = activityEntity(noSchedules = true)
    assertThat(activity.schedules()).isEmpty()

    activity.addSchedule(
      description = "Woodwork",
      internalLocation = Location(
        locationId = 1,
        internalLocationCode = "WW",
        description = "The wood work room description",
        locationType = "APP",
        agencyId = "MDI",
      ),
      capacity = 10,
      startDate = activity.startDate,
      runsOnBankHoliday = true,
      scheduleWeeks = 1,
    )

    assertThat(activity.schedules()).containsExactly(
      ActivitySchedule(
        activity = activity,
        description = "Woodwork",
        internalLocationId = 1,
        internalLocationCode = "WW",
        internalLocationDescription = "The wood work room description",
        capacity = 10,
        startDate = activity.startDate,
        runsOnBankHoliday = true,
        scheduleWeeks = 1,
      ),
    )
  }

  @Test
  fun `can add schedule to activity that starts and ends on same day`() {
    val activity = activityEntity(noSchedules = true, startDate = LocalDate.now(), endDate = LocalDate.now())
    assertThat(activity.schedules()).isEmpty()

    activity.addSchedule(
      description = "Woodwork",
      internalLocation = Location(
        locationId = 1,
        internalLocationCode = "WW",
        description = "The wood work room description",
        locationType = "APP",
        agencyId = "MDI",
      ),
      capacity = 10,
      startDate = activity.startDate,
      endDate = activity.endDate,
      runsOnBankHoliday = true,
      scheduleWeeks = 1,
    )

    with(activity.schedules().first()) {
      assertThat(this.activity).isEqualTo(activity)
      assertThat(description).isEqualTo("Woodwork")
      assertThat(internalLocationId).isEqualTo(1)
      assertThat(internalLocationCode).isEqualTo("WW")
      assertThat(internalLocationDescription).isEqualTo("The wood work room description")
      assertThat(capacity).isEqualTo(10)
      assertThat(startDate).isEqualTo(TimeSource.today())
      assertThat(endDate).isEqualTo(TimeSource.today())
      assertThat(runsOnBankHoliday).isTrue
      assertThat(scheduleWeeks).isEqualTo(1)
    }
  }

  @Test
  fun `cannot add schedule when start date is before that of the activity`() {
    val activity = activityEntity(noSchedules = true)
    assertThat(activity.schedules()).isEmpty()

    assertThatThrownBy {
      activity.addSchedule(
        description = "Woodwork",
        internalLocation = Location(
          locationId = 1,
          internalLocationCode = "WW",
          description = "The wood work room description",
          locationType = "APP",
          agencyId = "MDI",
        ),
        capacity = 10,
        startDate = activity.startDate.minusDays(1),
        runsOnBankHoliday = true,
        scheduleWeeks = 1,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The schedule start date '${activity.startDate.minusDays(1)}' cannot be before the activity start date ${activity.startDate}")
  }

  @Test
  fun `cannot add schedule when end date is after the end date of the activity`() {
    val activity =
      activityEntity(noSchedules = true).copy(startDate = LocalDate.now())
        .apply { endDate = LocalDate.now().plusDays(1) }
    assertThat(activity.schedules()).isEmpty()

    assertThatThrownBy {
      activity.addSchedule(
        description = "Woodwork",
        internalLocation = Location(
          locationId = 1,
          internalLocationCode = "WW",
          description = "The wood work room description",
          locationType = "APP",
          agencyId = "MDI",
        ),
        capacity = 10,
        startDate = activity.startDate,
        endDate = activity.endDate!!.plusDays(1),
        runsOnBankHoliday = true,
        scheduleWeeks = 1,
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
        internalLocation = Location(
          locationId = 1,
          internalLocationCode = "WW",
          description = "The wood work room description",
          locationType = "APP",
          agencyId = "MDI",
        ),
        capacity = 0,
        startDate = activity.startDate,
        runsOnBankHoliday = true,
        scheduleWeeks = 1,
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
      internalLocation = Location(
        locationId = 1,
        internalLocationCode = "WW",
        description = "The wood work room description",
        locationType = "APP",
        agencyId = "MDI",
      ),
      capacity = 10,
      startDate = activity.startDate,
      runsOnBankHoliday = true,
      scheduleWeeks = 1,
    )

    assertThatThrownBy {
      activity.addSchedule(
        description = " WooDwork ",
        internalLocation = Location(
          locationId = 1,
          internalLocationCode = "WW",
          description = "The wood work room description",
          locationType = "APP",
          agencyId = "MDI",
        ),
        capacity = 10,
        startDate = activity.startDate,
        runsOnBankHoliday = true,
        scheduleWeeks = 1,
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

  @Test
  fun `can add eligibility rules to activity`() {
    val activity = activityEntity(noEligibilityRules = true).also { assertThat(it.eligibilityRules()).isEmpty() }

    activity.addEligibilityRule(eligibilityRuleOver21)
    activity.addEligibilityRule(eligibilityRuleFemale)

    assertThat(activity.eligibilityRules()).containsExactlyInAnyOrder(
      ActivityEligibility(eligibilityRule = eligibilityRuleOver21, activity = activity),
      ActivityEligibility(eligibilityRule = eligibilityRuleFemale, activity = activity),
    )
  }

  @Test
  fun `cannot add duplicate eligibility rules to activity`() {
    val activity = activityEntity(noEligibilityRules = true)

    activity.addEligibilityRule(eligibilityRuleOver21)

    assertThatThrownBy { activity.addEligibilityRule(eligibilityRuleOver21) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Eligibility rule '${eligibilityRuleOver21.code}' already present on activity")
  }

  @Test
  fun `can add pay bands to paid activity`() {
    val activity = activityEntity(noPayBands = true, paid = true).also { assertThat(it.activityPay()).isEmpty() }

    activity.addPay(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBand = lowPayBand,
      rate = 30,
      pieceRate = 40,
      pieceRateItems = 50,
      startDate = null,
    )

    activity.addPay(
      incentiveNomisCode = "STD",
      incentiveLevel = "Standard",
      payBand = mediumPayBand,
      rate = 40,
      pieceRate = 50,
      pieceRateItems = 60,
      startDate = null,
    )

    assertThat(activity.activityPay()).containsExactlyInAnyOrder(
      ActivityPay(
        incentiveNomisCode = "BAS",
        incentiveLevel = "Basic",
        payBand = lowPayBand,
        rate = 30,
        pieceRate = 40,
        pieceRateItems = 50,
        activity = activity,
      ),
      ActivityPay(
        incentiveNomisCode = "STD",
        incentiveLevel = "Standard",
        payBand = mediumPayBand,
        rate = 40,
        pieceRate = 50,
        pieceRateItems = 60,
        activity = activity,
      ),
    )
  }

  @Test
  fun `can add pay bands to paid activity with a pay band and iep combinations to activity`() {
    val activity = activityEntity(noPayBands = true, paid = true).also { assertThat(it.activityPay()).isEmpty() }

    activity.addPay(
      incentiveNomisCode = "STD",
      incentiveLevel = "Standard",
      payBand = lowPayBand,
      rate = 30,
      pieceRate = 40,
      pieceRateItems = 50,
      startDate = null,
    )

    activity.addPay(
      incentiveNomisCode = "STD",
      incentiveLevel = "Standard",
      payBand = mediumPayBand,
      rate = 50,
      pieceRate = 50,
      pieceRateItems = 60,
      startDate = LocalDate.now(),
    )

    assertThat(activity.activityPay()).containsExactlyInAnyOrder(
      ActivityPay(
        incentiveNomisCode = "STD",
        incentiveLevel = "Standard",
        payBand = lowPayBand,
        rate = 30,
        pieceRate = 40,
        pieceRateItems = 50,
        activity = activity,
      ),
      ActivityPay(
        incentiveNomisCode = "STD",
        incentiveLevel = "Standard",
        payBand = mediumPayBand,
        rate = 40,
        pieceRate = 50,
        pieceRateItems = 60,
        activity = activity,
        startDate = LocalDate.now(),
      ),
    )
  }

  @Test
  fun `cannot add pay bands to unpaid activity`() {
    val activity = activityEntity(noPayBands = true, paid = false).also { assertThat(it.activityPay()).isEmpty() }

    assertThatThrownBy {
      activity.addPay(
        incentiveNomisCode = "BAS",
        incentiveLevel = "Basic",
        payBand = lowPayBand,
        rate = 30,
        pieceRate = 40,
        pieceRateItems = 50,
        startDate = null,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Unpaid activity 'Maths' cannot have pay rates added to it")
  }

  @Test
  fun `cannot add duplicate pay band, start date and iep combinations to activity`() {
    val activity = activityEntity(noPayBands = true).also { assertThat(it.activityPay()).isEmpty() }

    activity.addPay(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBand = lowPayBand,
      rate = 30,
      pieceRate = 40,
      pieceRateItems = 50,
      startDate = null,
    )

    val exception = assertThrows<IllegalArgumentException> {
      activity.addPay(
        incentiveNomisCode = "BAS",
        incentiveLevel = "Basic",
        payBand = lowPayBand,
        rate = 40,
        pieceRate = 50,
        pieceRateItems = 60,
        startDate = null,
      )
    }
    assertThat(exception.message).isEqualTo("The pay band, incentive level and start date combination must be unique for each pay rate")
  }

  @Test
  fun `get schedules on date when schedule has no suspensions`() {
    val activity = activityEntity()
    val schedule = activity.schedules().first()

    schedule.addSlot(
      weekNumber = 1,
      slotTimes = LocalTime.NOON to LocalTime.NOON.plusHours(1),
      DayOfWeek.entries.toSet(),
      timeSlot = TimeSlot.PM,
    )

    val schedules = activity.getSchedulesOnDay(schedule.startDate)

    assertThat(schedules).containsExactly(schedule)
  }

  @Test
  fun `get schedules on date excluding suspensions`() {
    val activity = activityEntity()
    val schedule = activity.schedules().first()

    schedule.addSlot(
      weekNumber = 1,
      slotTimes = LocalTime.NOON to LocalTime.NOON.plusHours(1),
      DayOfWeek.entries.toSet(),
      timeSlot = TimeSlot.PM,
    )

    val suspension = ActivityScheduleSuspension(
      activityScheduleSuspensionId = 1,
      activitySchedule = schedule,
      schedule.startDate,
    )

    assertThat(suspension.isSuspendedOn(schedule.startDate))

    schedule.suspensions.add(suspension)

    assertThat(activity.getSchedulesOnDay(schedule.startDate, includeSuspended = false)).isEmpty()
  }

  @Test
  fun `get schedules on date including suspensions`() {
    val activity = activityEntity()
    val schedule = activity.schedules().first()

    schedule.addSlot(
      weekNumber = 1,
      slotTimes = LocalTime.NOON to LocalTime.NOON.plusHours(1),
      setOf(*DayOfWeek.entries.toTypedArray()),
      timeSlot = TimeSlot.PM,
    )

    val suspension = ActivityScheduleSuspension(
      activityScheduleSuspensionId = 1,
      activitySchedule = schedule,
      schedule.startDate,
    )

    assertThat(suspension.isSuspendedOn(schedule.startDate))

    schedule.suspensions.add(suspension)

    assertThat(activity.getSchedulesOnDay(schedule.startDate, includeSuspended = true)).containsExactly(schedule)
  }

  @Test
  fun `get schedules is empty when schedule is schedule to start in future`() {
    val activity = activityEntity(noSchedules = true).also { assertThat(it.schedules()).isEmpty() }

    activity.addSchedule(
      description = "description",
      internalLocation = Location(
        locationId = 1,
        internalLocationCode = "RM1",
        description = "Room 1",
        locationType = "APP",
        agencyId = "MDI",
      ),
      capacity = 1,
      startDate = activity.startDate.plusDays(1),
      runsOnBankHoliday = true,
      scheduleWeeks = 1,
    ).apply {
      addSlot(
        weekNumber = 1,
        slotTimes = LocalTime.NOON to LocalTime.NOON.plusHours(1),
        DayOfWeek.entries.toSet(),
        timeSlot = TimeSlot.PM,
      )
    }.also {
      assertThat(activity.schedules()).hasSize(1)
    }

    assertThat(activity.getSchedulesOnDay(activity.startDate, includeSuspended = false)).isEmpty()
  }

  @Test
  fun `can fetch activity pay for a particular band and incentive code`() {
    val activity = activityEntity()

    assertThat(activity.activityPayFor(lowPayBand, "BAS")).isEqualTo(
      ActivityPay(
        activity = activity,
        incentiveNomisCode = "BAS",
        incentiveLevel = "Basic",
        payBand = lowPayBand,
        rate = 30,
        pieceRate = 40,
        pieceRateItems = 50,
      ),
    )
  }

  @Test
  fun `can fetch activity pay for a particular band and incentive code when there are multiple historic pays`() {
    val activity = activityEntity()

    activity.addPay(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBand = lowPayBand,
      rate = 33,
      pieceRate = 45,
      pieceRateItems = 55,
      startDate = LocalDate.now().minusDays(10),
    )

    activity.addPay(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBand = lowPayBand,
      rate = 34,
      pieceRate = 45,
      pieceRateItems = 55,
      startDate = LocalDate.now().minusDays(1),
    )

    val currentPay = activity.activityPayFor(lowPayBand, "BAS")

    assertThat(currentPay!!.rate).isEqualTo(34)
    assertThat(currentPay.startDate).isEqualTo(LocalDate.now().minusDays(1))
  }

  @Test
  fun `can fetch activity pay for a particular band and incentive code when there are multiple historic and future pays`() {
    val activity = activityEntity()

    activity.addPay(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBand = lowPayBand,
      rate = 35,
      pieceRate = 45,
      pieceRateItems = 55,
      startDate = LocalDate.now().minusDays(1),
    )

    activity.addPay(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBand = lowPayBand,
      rate = 65,
      pieceRate = 65,
      pieceRateItems = 65,
      startDate = LocalDate.now().plusDays(1),
    )

    activity.addPay(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBand = lowPayBand,
      rate = 75,
      pieceRate = 75,
      pieceRateItems = 75,
      startDate = LocalDate.now().plusDays(6),
    )

    val currentPay = activity.activityPayFor(lowPayBand, "BAS")

    assertThat(currentPay!!.rate).isEqualTo(35)
    assertThat(currentPay.startDate).isEqualTo(LocalDate.now().minusDays(1))
  }

  @Test
  fun `can add minimum education levels to activity`() {
    val activity =
      activityEntity(noMinimumEducationLevels = true).also { assertThat(it.activityMinimumEducationLevel()).isEmpty() }

    activity.addMinimumEducationLevel(
      educationLevelCode = "1",
      educationLevelDescription = "Reading Measure 1.0",
      studyAreaCode = "ENGLA",
      studyAreaDescription = "English Language",
    )

    assertThat(activity.activityMinimumEducationLevel()).containsExactlyInAnyOrder(
      ActivityMinimumEducationLevel(
        educationLevelCode = "1",
        educationLevelDescription = "Reading Measure 1.0",
        activity = activity,
        studyAreaCode = "ENGLA",
        studyAreaDescription = "English Language",
      ),
    )
  }

  @Test
  fun `isUnemployment flag true when activity is within the 'non work' category`() {
    val unemploymentActivity = activityEntity(category = notInWorkCategory)
    with(unemploymentActivity) {
      assertThat(isUnemployment()).isTrue
    }
  }

  @Test
  fun `isUnemployment flag false when activity is not within the 'non work' category`() {
    with(activityWithEndDate) {
      assertThat(isUnemployment()).isFalse
    }
  }

  @Test
  fun `end date must be on or after the start date`() {
    val activity = activityEntity(startDate = today, endDate = tomorrow)

    activity.endDate = null
    activity.endDate = today

    assertThatThrownBy {
      activity.endDate = yesterday
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity end date cannot be before activity start date.")
  }

  @Test
  fun `can update paid attribute on activity when not allocated`() {
    val activity = activitySchedule(activityEntity(noSchedules = true), noAllocations = true).activity

    activity.paid isBool true
    activity.activityPay() hasSize 1

    assertDoesNotThrow { activity.paid = false }

    activity.paid isBool false
    activity.activityPay() hasSize 0

    assertDoesNotThrow { activity.paid = true }

    activity.paid isBool true
    activity.activityPay() hasSize 0
  }

  @Test
  fun `cannot update paid attribute on paid activity when allocated`() {
    val activity = activityEntity()

    assertThatThrownBy { activity.paid = false }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Paid attribute cannot be updated for allocated activity '1'")

    // no op so allowed
    assertDoesNotThrow { activity.paid = true }
  }

  @Test
  fun `can update paid attribute on paid activity when all allocations are ended`() {
    val activity = activityEntity()
    activity.schedules().forEach {
      it.allocations().forEach {
        it.prisonerStatus = PrisonerStatus.ENDED
      }
    }

    assertDoesNotThrow { activity.paid = false }
  }

  @Test
  fun `cannot update paid attribute on unpaid activity when allocated`() {
    val activity = activityEntity(noPayBands = true, paid = false).also { assertThat(it.activityPay()).isEmpty() }

    assertThatThrownBy { activity.paid = true }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Paid attribute cannot be updated for allocated activity '1'")

    // no op so allowed
    assertDoesNotThrow { activity.paid = false }
  }
}
