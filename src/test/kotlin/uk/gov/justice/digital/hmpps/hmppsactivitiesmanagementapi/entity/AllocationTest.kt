package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class AllocationTest {

  private val today = TimeSource.today()
  private val yesterday = TimeSource.yesterday()
  private val tomorrow = TimeSource.tomorrow()

  private val prisonRegime = mapOf(
    TimeSlot.AM to Pair(LocalTime.of(0, 0), LocalTime.of(1, 0)),
    TimeSlot.PM to Pair(LocalTime.of(12, 0), LocalTime.of(13, 0)),
    TimeSlot.ED to Pair(LocalTime.of(18, 0), LocalTime.of(23, 59)),
  )

  @Test
  fun `check allocation ends`() {
    with(allocation().apply { endDate = tomorrow }) {
      assertThat(endsOn(yesterday)).isFalse
      assertThat(endsOn(today)).isFalse
      assertThat(endsOn(tomorrow)).isTrue
    }

    with(allocation().apply { endDate = null }) {
      assertThat(endsOn(yesterday)).isFalse
      assertThat(endsOn(today)).isFalse
      assertThat(endsOn(tomorrow)).isFalse
    }
  }

  @Test
  fun `check can deallocate active allocation`() {
    val dateTime = LocalDateTime.now()
    val allocation = allocation(startDate = LocalDate.now().minusDays(1), withExclusions = true)

    assertThat(allocation.status(PrisonerStatus.ACTIVE)).isTrue
    assertThat(allocation.deallocatedReason).isNull()
    assertThat(allocation.deallocatedBy).isNull()
    assertThat(allocation.deallocatedTime).isNull()
    assertThat(allocation.exclusions(ExclusionsFilter.ACTIVE)).hasSize(1)
    assertThat(allocation.exclusions(ExclusionsFilter.PRESENT)).hasSize(1)

    allocation.deallocateNowWithReason(DeallocationReason.ENDED)

    assertThat(allocation.status(PrisonerStatus.ENDED)).isTrue
    assertThat(allocation.deallocatedReason).isEqualTo(DeallocationReason.ENDED)
    assertThat(allocation.deallocatedBy).isEqualTo("Activities Management Service")
    assertThat(allocation.deallocatedTime).isCloseTo(dateTime, within(2, ChronoUnit.SECONDS))
    assertThat(allocation.exclusions(ExclusionsFilter.ACTIVE)).isEmpty()
    assertThat(allocation.exclusions(ExclusionsFilter.PRESENT)).hasSize(1)
  }

  @Test
  fun `check cannot deallocate if allocation already ended`() {
    val allocation = allocation().apply { deallocateNowWithReason(DeallocationReason.ENDED) }

    assertThatThrownBy { allocation.deallocateNowWithReason(DeallocationReason.ENDED) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Allocation with ID '0' is already deallocated.")
  }

  @Test
  fun `check default deallocation when deallocate today`() {
    val allocation = allocation(startDate = LocalDate.now().minusDays(1), withExclusions = true)

    allocation.deallocateNowOn(TimeSource.today())

    assertThat(allocation.status(PrisonerStatus.ENDED)).isTrue
    assertThat(allocation.deallocatedReason).isEqualTo(DeallocationReason.ENDED)
    assertThat(allocation.deallocatedBy).isEqualTo("Activities Management Service")
    assertThat(allocation.deallocatedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
    assertThat(allocation.exclusions(ExclusionsFilter.ACTIVE)).isEmpty()
    assertThat(allocation.exclusions(ExclusionsFilter.PRESENT)).hasSize(1)
    assertThat(allocation.endDate).isEqualTo(TimeSource.today())
  }

  @Test
  fun `check deallocation now with end before today`() {
    val allocation = allocation(startDate = LocalDate.now().minusDays(1), withExclusions = true)

    allocation.deallocateNowOn(TimeSource.yesterday())

    assertThat(allocation.status(PrisonerStatus.ENDED)).isTrue
    assertThat(allocation.deallocatedReason).isEqualTo(DeallocationReason.ENDED)
    assertThat(allocation.deallocatedBy).isEqualTo("Activities Management Service")
    assertThat(allocation.deallocatedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
    assertThat(allocation.exclusions(ExclusionsFilter.ACTIVE)).isEmpty()
    assertThat(allocation.exclusions(ExclusionsFilter.PRESENT)).hasSize(1)
    assertThat(allocation.endDate).isEqualTo(TimeSource.yesterday())
  }

  @Test
  fun `check planned deallocation takes precedence when deallocate today`() {
    val allocation = allocation().deallocateOn(LocalDate.now(), DeallocationReason.TRANSFERRED, "by test")

    allocation.deallocateNowOn(TimeSource.today())

    assertThat(allocation.status(PrisonerStatus.ENDED)).isTrue
    assertThat(allocation.deallocatedReason).isEqualTo(DeallocationReason.TRANSFERRED)
    assertThat(allocation.deallocatedBy).isEqualTo("by test")
    assertThat(allocation.deallocatedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
    assertThat(allocation.endDate).isEqualTo(TimeSource.today())
  }

  @Test
  fun `check can auto-suspend an active allocation`() {
    val allocation = allocation().also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    allocation.autoSuspend(today.atStartOfDay(), "Temporarily released from prison")

    with(allocation) {
      assertThat(prisonerStatus).isEqualTo(PrisonerStatus.AUTO_SUSPENDED)
      assertThat(suspendedBy).isEqualTo("Activities Management Service")
      assertThat(suspendedTime).isEqualTo(today.atStartOfDay())
      assertThat(suspendedReason).isEqualTo("Temporarily released from prison")
    }
  }

  @Test
  fun `check can auto-suspend a pending allocation`() {
    val allocation = allocation(startDate = TimeSource.tomorrow()).also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.PENDING) }

    allocation.autoSuspend(today.atStartOfDay(), "auto-suspension reason")

    with(allocation) {
      assertThat(prisonerStatus).isEqualTo(PrisonerStatus.AUTO_SUSPENDED)
      assertThat(suspendedBy).isEqualTo("Activities Management Service")
      assertThat(suspendedTime).isEqualTo(today.atStartOfDay())
      assertThat(suspendedReason).isEqualTo("auto-suspension reason")
    }
  }

  @Test
  fun `check cannot auto-suspend an ended allocation`() {
    val allocation = allocation().apply { deallocateNowWithReason(DeallocationReason.ENDED) }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ENDED) }

    assertThatThrownBy { allocation.autoSuspend(today.atStartOfDay(), "Temporarily released from prison") }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only auto-suspend active, pending or suspended allocations")
  }

  @Test
  fun `check cannot auto-suspend same allocation`() {
    val allocation = allocation().also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    allocation.autoSuspend(today.atStartOfDay(), "Temporarily released from prison")

    assertThatThrownBy { allocation.autoSuspend(today.atStartOfDay(), "Temporarily released from prison") }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only auto-suspend active, pending or suspended allocations")
  }

  @Test
  fun `check can unsuspend an auto-suspended allocation`() {
    val allocation = allocation()
      .apply { autoSuspend(today.atStartOfDay(), "Temporarily released from prison") }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.AUTO_SUSPENDED) }

    allocation.reactivateSuspension()

    with(allocation) {
      assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE)
      assertThat(suspendedBy).isNull()
      assertThat(suspendedTime).isNull()
      assertThat(suspendedReason).isNull()
    }
  }

  @Test
  fun `check cannot unsuspend an active allocation`() {
    val allocation = allocation().also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    assertThatThrownBy { allocation.reactivateSuspension() }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only reactivate suspended or auto-suspended allocations")
  }

  @Test
  fun `check cannot unsuspend an ended allocation`() {
    val allocation = allocation().apply { deallocateNowWithReason(DeallocationReason.ENDED) }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ENDED) }

    assertThatThrownBy { allocation.reactivateSuspension() }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only reactivate suspended or auto-suspended allocations")
  }

  @Test
  fun `planned deallocation must be on or after today`() {
    val allocation = allocation().also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    assertThatThrownBy {
      allocation.deallocateOn(yesterday, DeallocationReason.TRANSFERRED, "by test")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Planned deallocation date must not be in the past.")
  }

  @Test
  fun `planned deallocation must be on or before the schedule end date`() {
    val scheduleEndingToday: ActivitySchedule = mock {
      on { startDate } doReturn yesterday
      on { endDate } doReturn today
      on { isPaid() } doReturn true
    }

    val allocationNoEndDate = allocation().copy(activitySchedule = scheduleEndingToday).apply { endDate = null }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    assertThatThrownBy {
      allocationNoEndDate.deallocateOn(tomorrow, DeallocationReason.TRANSFERRED, "by test")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Planned deallocation date cannot be after activity schedule end date, $today.")
  }

  @Test
  fun `can plan deallocation for personal reasons`() {
    val allocation = allocation().copy(allocationId = 1).apply { endDate = null }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    assertThat(allocation.plannedDeallocation).isNull()
    assertThat(allocation.endDate).isNull()

    allocation.deallocateOn(tomorrow, DeallocationReason.TRANSFERRED, "by test")

    assertThat(allocation.endDate).isNull()

    with(allocation.plannedDeallocation!!) {
      assertThat(plannedDate).isEqualTo(tomorrow)
      assertThat(plannedBy).isEqualTo("by test")
      assertThat(plannedReason).isEqualTo(DeallocationReason.TRANSFERRED)
      assertThat(plannedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
    }

    assertThat(allocation.endsOn(tomorrow))
  }

  @Test
  fun `can update an existing planned deallocation`() {
    val schedule: ActivitySchedule = mock {
      on { startDate } doReturn yesterday
      on { endDate } doReturn tomorrow.plusWeeks(1)
      on { isPaid() } doReturn true
    }
    val allocation = allocation().copy(activitySchedule = schedule).apply { endDate = null }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    allocation.deallocateOn(tomorrow, DeallocationReason.TRANSFERRED, "by test")
    allocation.deallocateOn(tomorrow.plusDays(1), DeallocationReason.RELEASED, "by another test")

    with(allocation.plannedDeallocation!!) {
      assertThat(plannedDate).isEqualTo(tomorrow.plusDays(1))
      assertThat(plannedBy).isEqualTo("by another test")
      assertThat(plannedReason).isEqualTo(DeallocationReason.RELEASED)
      assertThat(plannedAt).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS))
    }

    assertThat(allocation.endsOn(tomorrow.plusDays(1)))
  }

  @Test
  fun `cannot plan deallocation if already ended`() {
    val allocation = allocation().copy(allocationId = 1).apply { endDate = null }
      .apply { deallocateNowWithReason(DeallocationReason.ENDED) }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ENDED) }

    assertThatThrownBy {
      allocation.deallocateOn(tomorrow, DeallocationReason.TRANSFERRED, "by test")
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Allocation with ID '1' is already deallocated.")
  }

  @Test
  fun `activate pending allocation`() {
    val allocation = allocation().copy(prisonerStatus = PrisonerStatus.PENDING)
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.PENDING) }

    allocation.activate()

    assertThat(allocation.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE)
  }

  @Test
  fun `allocation must be pending to activate`() {
    val allocation = allocation().copy(prisonerStatus = PrisonerStatus.PENDING)
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.PENDING) }

    allocation.activate()

    assertThatThrownBy {
      allocation.activate()
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only activate pending allocations")
  }

  @Test
  fun `is able to get allocation pay`() {
    val allocation = allocation()

    val allocationPay = allocation.allocationPay("BAS")

    assertThat(allocationPay).isEqualTo(
      ActivityPay(
        activity = allocation.activitySchedule.activity,
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
  fun `is able to get allocation pay with multiple activity pays`() {
    val allocation = allocation()

    allocation.activitySchedule.activity.addPay(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBand = lowPayBand,
      rate = 35,
      pieceRate = 45,
      pieceRateItems = 55,
      startDate = LocalDate.now().minusDays(5),
    )

    allocation.activitySchedule.activity.addPay(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBand = lowPayBand,
      rate = 50,
      pieceRate = 50,
      pieceRateItems = 60,
      startDate = LocalDate.now().minusDays(1),
    )

    allocation.activitySchedule.activity.addPay(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBand = lowPayBand,
      rate = 55,
      pieceRate = 50,
      pieceRateItems = 60,
      startDate = LocalDate.now().plusDays(4),
    )

    val allocationPay = allocation.allocationPay("BAS")

    assertThat(allocationPay).isEqualTo(
      ActivityPay(
        activity = allocation.activitySchedule.activity,
        incentiveNomisCode = "BAS",
        incentiveLevel = "Basic",
        payBand = lowPayBand,
        rate = 50,
        pieceRate = 50,
        pieceRateItems = 60,
      ),
    )
  }

  @Test
  fun `planned deallocation is updated when new end date is before planned`() {
    val allocation = allocation().apply {
      endDate = null
    }.deallocateOn(tomorrow, DeallocationReason.TRANSFERRED, "by test")

    assertThat(allocation.plannedDeallocation?.plannedDate).isEqualTo(tomorrow)

    allocation.apply {
      endDate = today
    }

    assertThat(allocation.endDate).isEqualTo(today)
    assertThat(allocation.plannedDeallocation?.plannedDate).isEqualTo(today)
  }

  @Test
  fun `planned deallocation is not updated when new end date is after planned date`() {
    val allocation = allocation().apply {
      endDate = null
    }.deallocateOn(tomorrow, DeallocationReason.TRANSFERRED, "by test")

    assertThat(allocation.plannedDeallocation?.plannedDate).isEqualTo(tomorrow)

    allocation.apply {
      endDate = tomorrow.plusDays(1)
    }

    assertThat(allocation.endDate).isEqualTo(tomorrow.plusDays(1))
    assertThat(allocation.plannedDeallocation?.plannedDate).isEqualTo(tomorrow)
  }

  @Test
  fun `allocation end date must be on or after the start date`() {
    val allocation = allocation().copy(prisonerNumber = "123456")

    allocation.endDate = allocation.startDate
    allocation.endDate = null

    assertThatThrownBy {
      allocation.endDate = allocation.startDate.minusDays(1)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation end date for prisoner 123456 cannot be before allocation start date.")
  }

  @Test
  fun `allocation end date must be on or after the start date unless ended`() {
    val allocation = allocation().copy(prisonerNumber = "123456", startDate = TimeSource.tomorrow()).also { it.endDate isEqualTo null }

    assertDoesNotThrow { allocation.deallocateNowOn(TimeSource.today()) }

    allocation.startDate isEqualTo TimeSource.tomorrow()
    allocation.endDate isEqualTo TimeSource.today()
  }

  @Test
  fun `update exclusions - add and remove an exclusion`() {
    val allocation = allocation()
    allocation.exclusions(ExclusionsFilter.ACTIVE) hasSize 0

    val exclusion = allocation.updateExclusion(allocation.activitySchedule.slots().first(), setOf(DayOfWeek.MONDAY), tomorrow)

    allocation.exclusions(ExclusionsFilter.ACTIVE) hasSize 1
    with(exclusion!!) {
      getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY)
    }

    val updatedExclusion = allocation.updateExclusion(allocation.activitySchedule.slots().first(), setOf(), tomorrow)

    allocation.exclusions(ExclusionsFilter.ACTIVE) hasSize 0
    updatedExclusion isEqualTo null
  }

  @Test
  fun `add exclusions - cannot add exclusions where the activity does not run`() {
    val allocation = allocation()

    with(allocation.activitySchedule) {
      val slot = slots().single()
      slot.slotTimes() isEqualTo (LocalTime.MIDNIGHT to LocalTime.MIDNIGHT.plusHours(1))
      slot.weekNumber isEqualTo 1
      slot.getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY)
    }

    assertThatThrownBy {
      allocation.addExclusion(
        Exclusion.valueOf(
          allocation = allocation,
          weekNumber = 1,
          daysOfWeek = setOf(DayOfWeek.MONDAY),
          timeSlot = TimeSlot.PM,
        ),
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot set exclusions where the activity does not run")

    assertThatThrownBy {
      allocation.addExclusion(
        Exclusion.valueOf(
          allocation = allocation,
          weekNumber = 2,
          daysOfWeek = setOf(DayOfWeek.MONDAY),
          timeSlot = TimeSlot.AM,
        ),
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot set exclusions where the activity does not run")

    assertThatThrownBy {
      allocation.addExclusion(
        Exclusion.valueOf(
          allocation = allocation,
          weekNumber = 1,
          daysOfWeek = setOf(DayOfWeek.TUESDAY),
          timeSlot = TimeSlot.PM,
        ),
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot set exclusions where the activity does not run")
  }

  @Test
  fun `syncExclusionsWithScheduleSlots - ends present exclusions which do not have a matching slot`() {
    val allocation = allocation(startDate = LocalDate.now(), withExclusions = true)

    allocation.exclusions(ExclusionsFilter.PRESENT) hasSize 1
    allocation.exclusions(ExclusionsFilter.ACTIVE) hasSize 1

    allocation.syncExclusionsWithScheduleSlots(
      listOf(
        ActivityScheduleSlot.valueOf(allocation.activitySchedule, 1, prisonRegime[TimeSlot.AM]!!, setOf(DayOfWeek.TUESDAY), TimeSlot.AM),
      ),
    )

    allocation.exclusions(ExclusionsFilter.PRESENT) hasSize 1
    allocation.exclusions(ExclusionsFilter.ACTIVE) hasSize 0
  }

  @Test
  fun `syncExclusionsWithScheduleSlots - removes future exclusions which do not have a matching slot`() {
    val allocation = allocation(withExclusions = true)

    allocation.exclusions(ExclusionsFilter.FUTURE) hasSize 1
    allocation.exclusions(ExclusionsFilter.ACTIVE) hasSize 1

    allocation.syncExclusionsWithScheduleSlots(
      listOf(
        ActivityScheduleSlot.valueOf(allocation.activitySchedule, 1, prisonRegime[TimeSlot.AM]!!, setOf(DayOfWeek.TUESDAY), TimeSlot.AM),
      ),
    )

    allocation.exclusions(ExclusionsFilter.FUTURE) hasSize 0
    allocation.exclusions(ExclusionsFilter.ACTIVE) hasSize 0
  }

  @Test
  fun `syncExclusionsWithScheduleSlots - updates present exclusions with the new days`() {
    val activity = activityEntity()
    val schedule = activitySchedule(activity, daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
    val slot = schedule.slots().first()
    val allocation = schedule.allocations().first().apply {
      addExclusion(
        Exclusion.valueOf(
          allocation = this,
          weekNumber = slot.weekNumber,
          daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
          startDate = LocalDate.now(),
          timeSlot = slot.timeSlot,
        ),
      )
    }

    with(allocation) {
      exclusions(ExclusionsFilter.PRESENT) hasSize 1
      exclusions(ExclusionsFilter.ACTIVE) hasSize 1
      exclusions(ExclusionsFilter.ACTIVE).first().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
    }

    allocation.syncExclusionsWithScheduleSlots(
      listOf(
        ActivityScheduleSlot.valueOf(
          allocation.activitySchedule,
          1,
          prisonRegime[TimeSlot.AM]!!,
          setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
          TimeSlot.AM,
        ),
        ActivityScheduleSlot.valueOf(
          allocation.activitySchedule,
          1,
          prisonRegime[TimeSlot.AM]!!,
          setOf(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
          TimeSlot.AM,
        ),

      ),
    )

    with(allocation) {
      exclusions(ExclusionsFilter.PRESENT) hasSize 1
      exclusions(ExclusionsFilter.PRESENT).first().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
      exclusions(ExclusionsFilter.ACTIVE) hasSize 1
      exclusions(ExclusionsFilter.ACTIVE).first().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
    }
  }

  @Test
  fun `syncExclusionsWithScheduleSlots - updates future exclusions with the new days`() {
    val activity = activityEntity()
    val schedule = activitySchedule(activity, daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
    val slot = schedule.slots().first()
    val allocation = schedule.allocations().first().apply {
      addExclusion(
        Exclusion.valueOf(
          allocation = this,
          weekNumber = slot.weekNumber,
          daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
          startDate = LocalDate.now().plusDays(1),
          timeSlot = slot.timeSlot,
        ),
      )
    }

    with(allocation) {
      exclusions(ExclusionsFilter.ACTIVE) hasSize 1
      exclusions(ExclusionsFilter.FUTURE) hasSize 1
      exclusions(ExclusionsFilter.FUTURE).first().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
    }

    allocation.syncExclusionsWithScheduleSlots(
      listOf(
        ActivityScheduleSlot.valueOf(allocation.activitySchedule, 1, prisonRegime[TimeSlot.AM]!!, setOf(DayOfWeek.MONDAY), TimeSlot.AM),
      ),
    )

    with(allocation) {
      exclusions(ExclusionsFilter.ACTIVE) hasSize 1
      exclusions(ExclusionsFilter.ACTIVE).first().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY)
      exclusions(ExclusionsFilter.FUTURE) hasSize 1
      exclusions(ExclusionsFilter.FUTURE).first().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY)
    }
  }

  @Test
  fun `syncExclusionsWithScheduleSlots - removes present exclusions on a multi-week model where they do not map to nomis data model`() {
    val activity = activityEntity(noSchedules = true)
    val schedule = activitySchedule(activity, noSlots = true, scheduleWeeks = 2)

    schedule.addSlot(
      weekNumber = 1,
      slotTimes = prisonRegime[TimeSlot.PM]!!,
      daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
      timeSlot = TimeSlot.PM,
    )

    schedule.addSlot(
      weekNumber = 2,
      slotTimes = prisonRegime[TimeSlot.PM]!!,
      daysOfWeek = setOf(DayOfWeek.THURSDAY),
      timeSlot = TimeSlot.PM,
    )

    val allocation = schedule.allocatePrisoner(
      prisonerNumber = "A1111BB".toPrisonerNumber(),
      bookingId = 20002,
      payBand = lowPayBand,
      allocatedBy = "Mr Blogs",
      startDate = activity.startDate,
    ).apply {
      addExclusion(
        Exclusion.valueOf(
          allocation = this,
          weekNumber = 1,
          daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
          startDate = LocalDate.now(),
          timeSlot = TimeSlot.PM,
        ),
      )
    }

    with(allocation) {
      exclusions(ExclusionsFilter.PRESENT).single().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
      exclusions(ExclusionsFilter.ACTIVE).single().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
    }

    allocation.syncExclusionsWithScheduleSlots(
      listOf(
        ActivityScheduleSlot.valueOf(allocation.activitySchedule, 1, prisonRegime[TimeSlot.PM]!!, setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), TimeSlot.PM),
        ActivityScheduleSlot.valueOf(allocation.activitySchedule, 2, prisonRegime[TimeSlot.PM]!!, setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), TimeSlot.PM),
      ),
    )

    with(allocation) {
      exclusions(ExclusionsFilter.PRESENT).single().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
      exclusions(ExclusionsFilter.ACTIVE).single().getDaysOfWeek() isEqualTo setOf(DayOfWeek.FRIDAY)
    }
  }

  @Test
  fun `syncExclusionsWithScheduleSlots - removes future exclusions on a multi-week model where they do not map to nomis data model`() {
    val activity = activityEntity(noSchedules = true)
    val schedule = activitySchedule(activity, noSlots = true, scheduleWeeks = 2)

    schedule.addSlot(
      weekNumber = 1,
      slotTimes = prisonRegime[TimeSlot.PM]!!,
      daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
      timeSlot = TimeSlot.PM,
    )

    schedule.addSlot(
      weekNumber = 2,
      slotTimes = prisonRegime[TimeSlot.PM]!!,
      daysOfWeek = setOf(DayOfWeek.THURSDAY),
      timeSlot = TimeSlot.PM,
    )

    val allocation = schedule.allocatePrisoner(
      prisonerNumber = "A1111BB".toPrisonerNumber(),
      bookingId = 20002,
      payBand = lowPayBand,
      allocatedBy = "Mr Blogs",
      startDate = activity.startDate,
    ).apply {
      addExclusion(
        Exclusion.valueOf(
          allocation = this,
          weekNumber = 1,
          daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
          timeSlot = TimeSlot.PM,
        ),
      )
    }

    with(allocation) {
      exclusions(ExclusionsFilter.FUTURE).single().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
      exclusions(ExclusionsFilter.ACTIVE).single().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
    }

    allocation.syncExclusionsWithScheduleSlots(
      listOf(
        ActivityScheduleSlot.valueOf(allocation.activitySchedule, 1, prisonRegime[TimeSlot.PM]!!, setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), TimeSlot.PM),
        ActivityScheduleSlot.valueOf(allocation.activitySchedule, 2, prisonRegime[TimeSlot.PM]!!, setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), TimeSlot.PM),
      ),
    )

    with(allocation) {
      exclusions(ExclusionsFilter.FUTURE).single().getDaysOfWeek() isEqualTo setOf(DayOfWeek.FRIDAY)
      exclusions(ExclusionsFilter.ACTIVE).single().getDaysOfWeek() isEqualTo setOf(DayOfWeek.FRIDAY)
    }
  }

  @Test
  fun `allocation starting today can be attended today`() {
    val allocation = allocation(startDate = TimeSource.today())

    allocation.canAttendOn(TimeSource.today(), TimeSlot.AM) isBool true
  }

  @Test
  fun `allocation starting today cannot be attended yesterday`() {
    val allocation = allocation(startDate = TimeSource.today())

    allocation.canAttendOn(TimeSource.yesterday(), TimeSlot.AM) isBool false
  }

  @Test
  fun `allocation starting yesterday can be attended today`() {
    val allocation = allocation(startDate = TimeSource.yesterday())

    allocation.canAttendOn(TimeSource.today(), TimeSlot.AM) isBool true
  }

  @Test
  fun `allocation ending today can be attended today`() {
    val allocation = allocation(startDate = TimeSource.today()).apply { endDate = TimeSource.today() }

    allocation.canAttendOn(TimeSource.today(), TimeSlot.AM) isBool true
  }

  @Test
  fun `allocation ending yesterday can be attended yesterday`() {
    val allocation = allocation(startDate = TimeSource.yesterday()).apply { endDate = TimeSource.yesterday() }

    allocation.canAttendOn(TimeSource.yesterday(), TimeSlot.AM) isBool true
  }

  @Test
  fun `allocation ending yesterday cannot be attended today`() {
    val allocation = allocation(startDate = TimeSource.yesterday()).apply { endDate = TimeSource.yesterday() }

    allocation.canAttendOn(TimeSource.today(), TimeSlot.AM) isBool false
  }

  @Test
  fun `allocation planned to end yesterday cannot be attended today`() {
    val allocation = allocation(startDate = TimeSource.yesterday()).apply {
      deallocateOn(TimeSource.today(), DeallocationReason.DISMISSED, "test")
      // Cannot add a planned deallocation with yesterday's date so have to override it manually in the test.
      plannedDeallocation?.plannedDate = TimeSource.yesterday()
    }

    allocation.canAttendOn(TimeSource.today(), TimeSlot.AM) isBool false
  }

  @Test
  fun `allocation starting tomorrow cannot be attended today`() {
    val allocation = allocation(startDate = TimeSource.tomorrow())

    allocation.canAttendOn(TimeSource.today(), TimeSlot.AM) isBool false
  }

  @Test
  fun `allocation deallocated cannot be attended`() {
    val allocation = allocation().deallocateNowOn(TimeSource.today())

    allocation.canAttendOn(TimeSource.today(), TimeSlot.AM) isBool false
  }

  @Test
  fun `allocation with exclusion cannot be attended`() {
    val allocation = activitySchedule(activity = activityEntity(), daysOfWeek = setOf(TimeSource.tomorrow().dayOfWeek)).allocations().first()

    val timeSlot = allocation.activitySchedule.slots().first().timeSlot

    allocation.canAttendOn(TimeSource.tomorrow(), timeSlot) isBool true

    allocation.updateExclusion(allocation.activitySchedule.slots().first(), setOf(TimeSource.tomorrow().dayOfWeek), today)

    allocation.canAttendOn(TimeSource.tomorrow(), timeSlot) isBool false
  }

  @Test
  fun `allocation for paid activity must have a pay band`() {
    assertThatThrownBy {
      Allocation(
        activitySchedule = activityEntity(activityId = 1, paid = true).schedules().first(),
        allocatedBy = "test",
        allocatedTime = TimeSource.now(),
        prisonerNumber = "123",
        bookingId = 1,
        startDate = TimeSource.today(),
        initialPayBand = null,
      )
    }.isInstanceOf(IllegalArgumentException::class.java).hasMessage("Pay band must be provided for paid activity ID '1'")

    assertThatThrownBy {
      Allocation(
        activitySchedule = activityEntity(activityId = 2, paid = true).schedules().first(),
        allocatedBy = "test",
        allocatedTime = TimeSource.now(),
        prisonerNumber = "123",
        bookingId = 1,
        startDate = TimeSource.today(),
        initialPayBand = lowPayBand,
      ).apply {
        payBand = null
      }
    }.isInstanceOf(IllegalArgumentException::class.java).hasMessage("Pay band must be provided for paid activity ID '2'")
  }

  @Test
  fun `allocation for unpaid activity cannot have a pay band`() {
    assertThatThrownBy {
      Allocation(
        activitySchedule = activityEntity(activityId = 1, paid = false, noPayBands = true).schedules().first(),
        allocatedBy = "test",
        allocatedTime = TimeSource.now(),
        prisonerNumber = "123",
        bookingId = 1,
        startDate = TimeSource.today(),
        initialPayBand = lowPayBand,
      )
    }.isInstanceOf(IllegalArgumentException::class.java).hasMessage("Pay band must not be provided for unpaid activity ID '1'")

    assertThatThrownBy {
      Allocation(
        activitySchedule = activityEntity(activityId = 2, paid = false, noPayBands = true).schedules().first(),
        allocatedBy = "test",
        allocatedTime = TimeSource.now(),
        prisonerNumber = "123",
        bookingId = 1,
        startDate = TimeSource.today(),
        initialPayBand = null,
      ).apply {
        payBand = lowPayBand
      }
    }.isInstanceOf(IllegalArgumentException::class.java).hasMessage("Pay band must not be provided for unpaid activity ID '2'")
  }

  @Test
  fun `plannedSuspension - fetches the latest planned suspension`() {
    val allocation = allocation(startDate = TimeSource.yesterday(), withPlannedSuspensions = true)
    allocation.plannedSuspension()!!.endDate() isEqualTo null
  }

  @Test
  fun `plannedSuspension - does not return a future suspension if the start and end dates are the same`() {
    val allocation = allocation(startDate = TimeSource.tomorrow(), withPlannedSuspensions = true).apply {
      plannedSuspension()!!.endOn(TimeSource.tomorrow(), "Test")
    }

    allocation.plannedSuspension() isEqualTo null
  }

  @Test
  fun `addPlannedSuspension - throws error if the given planned suspension does not belong to the allocation`() {
    val allocation1 = allocation(startDate = TimeSource.yesterday())
    val allocation2 = allocation(startDate = TimeSource.yesterday())

    val plannedSuspension = PlannedSuspension(
      allocation = allocation1,
      plannedStartDate = allocation1.startDate,
      plannedBy = "Test",
    )

    assertThatThrownBy { allocation2.addPlannedSuspension(plannedSuspension) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add this suspension associated to the allocation with id 0 to allocation with id 0")
  }

  @Test
  fun `isCurrentlySuspended - returns true when a planned suspension is active`() {
    val allocation = allocation(startDate = TimeSource.yesterday(), withPlannedSuspensions = true)
    allocation.isCurrentlySuspended() isBool true
  }

  @Test
  fun `isCurrentlySuspended - returns false when a planned suspension is not active`() {
    val allocation = allocation(startDate = TimeSource.yesterday()).apply {
      addPlannedSuspension(
        PlannedSuspension(
          allocation = this,
          plannedStartDate = LocalDate.now().plusWeeks(1),
          plannedBy = "Test",
        ),
      )
    }
    allocation.isCurrentlySuspended() isBool false
  }

  @Test
  fun `isCurrentlySuspended - returns false when a suspension is not planned`() {
    val allocation = allocation(startDate = TimeSource.yesterday())
    allocation.isCurrentlySuspended() isBool false
  }

  @Test
  fun `check cannot deallocated in future`() {
    val allocation = allocation().apply { endDate = TimeSource.today() }

    assertThatThrownBy {
      allocation.deallocateNowOn(TimeSource.tomorrow())
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation '0' cannot be deallocated with the future date '${TimeSource.tomorrow().toIsoDate()}'")
  }
}
