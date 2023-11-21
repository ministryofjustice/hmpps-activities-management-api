package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
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

  @Test
  fun `check allocation ends`() {
    with(allocation().apply { endDate = tomorrow }) {
      assertThat(ends(yesterday)).isFalse
      assertThat(ends(today)).isFalse
      assertThat(ends(tomorrow)).isTrue
    }

    with(allocation().apply { endDate = null }) {
      assertThat(ends(yesterday)).isFalse
      assertThat(ends(today)).isFalse
      assertThat(ends(tomorrow)).isFalse
    }
  }

  @Test
  fun `check can deallocate active allocation`() {
    val dateTime = LocalDateTime.now()
    val allocation = allocation()

    assertThat(allocation.status(PrisonerStatus.ACTIVE)).isTrue
    assertThat(allocation.deallocatedReason).isNull()
    assertThat(allocation.deallocatedBy).isNull()
    assertThat(allocation.deallocatedTime).isNull()

    allocation.deallocateNowWithReason(DeallocationReason.ENDED)

    assertThat(allocation.status(PrisonerStatus.ENDED)).isTrue
    assertThat(allocation.deallocatedReason).isEqualTo(DeallocationReason.ENDED)
    assertThat(allocation.deallocatedBy).isEqualTo("Activities Management Service")
    assertThat(allocation.deallocatedTime).isCloseTo(dateTime, within(2, ChronoUnit.SECONDS))
  }

  @Test
  fun `check cannot deallocate if allocation already ended`() {
    val allocation = allocation().apply { deallocateNowWithReason(DeallocationReason.ENDED) }

    assertThatThrownBy { allocation.deallocateNowWithReason(DeallocationReason.ENDED) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Allocation with ID '0' is already deallocated.")
  }

  @Test
  fun `check default deallocation when deallocate now`() {
    val allocation = allocation()

    allocation.deallocateNow()

    assertThat(allocation.status(PrisonerStatus.ENDED)).isTrue
    assertThat(allocation.deallocatedReason).isEqualTo(DeallocationReason.ENDED)
    assertThat(allocation.deallocatedBy).isEqualTo("Activities Management Service")
    assertThat(allocation.deallocatedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
  }

  @Test
  fun `check planned deallocation takes precedence when deallocate now`() {
    val allocation = allocation().deallocateOn(LocalDate.now(), DeallocationReason.TRANSFERRED, "by test")

    allocation.deallocateNow()

    assertThat(allocation.status(PrisonerStatus.ENDED)).isTrue
    assertThat(allocation.deallocatedReason).isEqualTo(DeallocationReason.TRANSFERRED)
    assertThat(allocation.deallocatedBy).isEqualTo("by test")
    assertThat(allocation.deallocatedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
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
      .hasMessage("You can only auto-suspend active and pending allocations")
  }

  @Test
  fun `check cannot auto-suspend same allocation`() {
    val allocation = allocation().also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    allocation.autoSuspend(today.atStartOfDay(), "Temporarily released from prison")

    assertThatThrownBy { allocation.autoSuspend(today.atStartOfDay(), "Temporarily released from prison") }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only auto-suspend active and pending allocations")
  }

  @Test
  fun `check can unsuspend an auto-suspended allocation`() {
    val allocation = allocation()
      .apply { autoSuspend(today.atStartOfDay(), "Temporarily released from prison") }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.AUTO_SUSPENDED) }

    allocation.reactivateAutoSuspensions()

    with(allocation) {
      assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE)
      assertThat(suspendedBy).isNull()
      assertThat(suspendedTime).isNull()
      assertThat(suspendedReason).isNull()
    }
  }

  @Test
  fun `check cannot unsuspend a manual user suspended allocation`() {
    val allocation = allocation()
      .apply { userSuspend(today.atStartOfDay(), "User suspension", "A user") }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.SUSPENDED) }

    assertThatThrownBy { allocation.reactivateAutoSuspensions() }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only reactivate auto-suspended allocations")
  }

  @Test
  fun `check cannot unsuspend an active allocation`() {
    val allocation = allocation().also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    assertThatThrownBy { allocation.reactivateAutoSuspensions() }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only reactivate auto-suspended allocations")
  }

  @Test
  fun `check cannot unsuspend an ended allocation`() {
    val allocation = allocation().apply { deallocateNowWithReason(DeallocationReason.ENDED) }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ENDED) }

    assertThatThrownBy { allocation.reactivateAutoSuspensions() }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only reactivate auto-suspended allocations")
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

    assertThat(allocation.ends(tomorrow))
  }

  @Test
  fun `can update an exiting planned deallocation`() {
    val schedule: ActivitySchedule = mock {
      on { startDate } doReturn yesterday
      on { endDate } doReturn tomorrow.plusWeeks(1)
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

    assertThat(allocation.ends(tomorrow.plusDays(1)))
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

    assertDoesNotThrow { allocation.deallocateNow() }

    allocation.startDate isEqualTo TimeSource.tomorrow()
    allocation.endDate isEqualTo TimeSource.today()
  }

  @Test
  fun `update exclusions - add and remove an exclusion`() {
    val allocation = allocation()
    allocation.exclusions() hasSize 0

    val exclusion = allocation.updateExclusion(allocation.activitySchedule.slots().first(), setOf(DayOfWeek.MONDAY))

    allocation.exclusions() hasSize 1
    with(exclusion!!) {
      getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY)
    }

    val updatedExclusion = allocation.updateExclusion(allocation.activitySchedule.slots().first(), setOf())

    allocation.exclusions() hasSize 0
    updatedExclusion isEqualTo null
  }

  @Test
  fun `update exclusions - cannot add exclusions for same day and time slot over multiple weeks`() {
    val activity = activityEntity(noSchedules = true)
    val schedule = activitySchedule(activity, noSlots = true, scheduleWeeks = 2)

    schedule.addSlot(
      weekNumber = 1,
      startTime = LocalTime.NOON,
      endTime = LocalTime.NOON.plusHours(1),
      daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
    )

    schedule.addSlot(
      weekNumber = 2,
      startTime = LocalTime.NOON,
      endTime = LocalTime.NOON.plusHours(1),
      daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
    )

    val allocation = schedule.allocatePrisoner(
      prisonerNumber = "A1111BB".toPrisonerNumber(),
      bookingId = 20002,
      payBand = lowPayBand,
      allocatedBy = "Mr Blogs",
      startDate = activity.startDate,
    )

    allocation.exclusions() hasSize 0

    allocation.updateExclusion(allocation.activitySchedule.slots().first(), setOf(DayOfWeek.MONDAY))

    allocation.exclusions() hasSize 1

    assertThatThrownBy { allocation.updateExclusion(allocation.activitySchedule.slots().last(), setOf(DayOfWeek.MONDAY)) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Exclusions cannot be added for the same day and time slot over multiple weeks.")

    allocation.exclusions() hasSize 1

    allocation.updateExclusion(allocation.activitySchedule.slots().last(), setOf(DayOfWeek.THURSDAY))

    allocation.exclusions() hasSize 2
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
  fun `allocation starting tomorrow cannot be attended today`() {
    val allocation = allocation(startDate = TimeSource.tomorrow())

    allocation.canAttendOn(TimeSource.today(), TimeSlot.AM) isBool false
  }

  @Test
  fun `allocation deallocated cannot be attended`() {
    val allocation = allocation().deallocateNow()

    allocation.canAttendOn(TimeSource.today(), TimeSlot.AM) isBool false
  }

  @Test
  fun `allocation with exclusion cannot be attended`() {
    val allocation = activitySchedule(activity = activityEntity(), daysOfWeek = setOf(TimeSource.today().dayOfWeek)).allocations().first()

    allocation.canAttendOn(TimeSource.today(), allocation.activitySchedule.slots().first().timeSlot()) isBool true

    allocation.updateExclusion(allocation.activitySchedule.slots().first(), setOf(TimeSource.today().dayOfWeek))

    allocation.canAttendOn(TimeSource.today(), allocation.activitySchedule.slots().first().timeSlot()) isBool false
  }
}
