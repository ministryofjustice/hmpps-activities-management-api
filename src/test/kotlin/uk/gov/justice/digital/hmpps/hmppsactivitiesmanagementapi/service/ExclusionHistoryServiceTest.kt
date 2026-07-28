package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot.AM
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot.ED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot.PM
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ExclusionRevision
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RevisionType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RevisionType.REMOVED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ExclusionHistoryAuditRow
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ExclusionRepository
import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDateTime

class ExclusionHistoryServiceTest {

  private companion object {
    private const val ADDED = 0
    private const val MODIFIED = 1
    private const val DELETED = 2

    const val ALLOCATION_ID = 1L
  }

  val exclusionRepository: ExclusionRepository = mockk()

  val exclusionHistoryService = ExclusionHistoryService(exclusionRepository)

  val allocation: Allocation = mockk()

  @BeforeEach
  fun setUp() {
    every { allocation.allocationId } returns ALLOCATION_ID
  }

  @Test
  fun `should return history`() {
    // revision 1
    val removedMondayAM = auditRow(exclusionRevisionType = MODIFIED, exclusionDaysOfWeekRevisionType = DELETED)
    val addedMondayAM = auditRow()
    val removedTuesdayAM = auditRow(dayOfWeek = TUESDAY, exclusionRevisionType = DELETED, exclusionDaysOfWeekRevisionType = DELETED)
    val addedTuesdayPM = auditRow(dayOfWeek = TUESDAY, timeSlot = PM)

    // revision 2
    val revision2DateTime = LocalDateTime.parse("2026-06-26T10:15:30")
    val addedWednesdayED = auditRow(revision = 2, weekNumber = 2, username = "SMITHJ", dayOfWeek = WEDNESDAY, timeSlot = ED, revisionDateTime = revision2DateTime)
    val addedTuesdayAM = auditRow(revision = 2, weekNumber = 2, username = "SMITHJ", dayOfWeek = TUESDAY, revisionDateTime = revision2DateTime)

    // revision 3
    val revision3DateTime = LocalDateTime.parse("2026-06-30T12:00:01")
    val addedThursdayAM = auditRow(revision = 3, revisionDateTime = revision3DateTime, dayOfWeek = THURSDAY)
    val addedThursdayPM = auditRow(revision = 3, revisionDateTime = revision3DateTime, dayOfWeek = THURSDAY, timeSlot = PM)

    every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns
      listOf(
        removedMondayAM,
        addedMondayAM,
        removedTuesdayAM,
        addedTuesdayPM,
        addedWednesdayED,
        addedTuesdayAM,
        addedThursdayAM,
        addedThursdayPM,
      )

    val history = exclusionHistoryService.findHistory(allocation)

    assertThat(history).containsExactly(
      exclusionRevision(revision = 3, dayOfWeek = THURSDAY, timeSlots = listOf(AM, PM), updatedDateTime = revision3DateTime),
      exclusionRevision(revision = 2, weekNumber = 2, dayOfWeek = TUESDAY, updatedBy = "SMITHJ", updatedDateTime = revision2DateTime),
      exclusionRevision(revision = 2, weekNumber = 2, dayOfWeek = WEDNESDAY, timeSlots = listOf(ED), updatedBy = "SMITHJ", updatedDateTime = revision2DateTime),
      exclusionRevision(dayOfWeek = TUESDAY, revisionType = REMOVED),
      exclusionRevision(dayOfWeek = TUESDAY, timeSlots = listOf(PM)),
    )
  }

  @Nested
  @DisplayName("Adding exclusions across separate sessions")
  inner class AddingExclusionsAcrossSessions {
    @Test
    fun `should return ADDED revision type when adding exclusions in separate sessions`() {
      // Session 1: Add Monday AM exclusion - exclusion entity is new (ADDED)
      val session1MondayAM = auditRow(
        dayOfWeek = MONDAY,
        exclusionRevisionType = ADDED,
        exclusionDaysOfWeekRevisionType = ADDED,
      )

      // Session 2: Add Tuesday AM exclusion - exclusion entity already exists so is MODIFIED, but day is ADDED
      val session2TuesdayAM = auditRow(
        revision = 2,
        dayOfWeek = TUESDAY,
        exclusionRevisionType = MODIFIED,
        exclusionDaysOfWeekRevisionType = ADDED,
        username = "USER2",
        revisionDateTime = LocalDateTime.parse("2026-06-26T11:00:00"),
      )

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns
        listOf(session1MondayAM, session2TuesdayAM)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revision = 2, dayOfWeek = TUESDAY, updatedBy = "USER2", updatedDateTime = LocalDateTime.parse("2026-06-26T11:00:00")),
        exclusionRevision(dayOfWeek = MONDAY),
      )

      // Both should be ADDED
      assertThat(history).allMatch { it.revisionType == RevisionType.ADDED }
    }

    @Test
    fun `should return ADDED revision type when adding multiple exclusions across three sessions`() {
      // Session 1: Add Monday AM
      val session1MondayAM = auditRow(
        dayOfWeek = MONDAY,
        exclusionRevisionType = ADDED,
        exclusionDaysOfWeekRevisionType = ADDED,
      )

      // Session 2: Add Tuesday AM - exclusion entity is MODIFIED
      val session2TuesdayAM = auditRow(
        revision = 2,
        dayOfWeek = TUESDAY,
        exclusionRevisionType = MODIFIED,
        exclusionDaysOfWeekRevisionType = ADDED,
        username = "USER2",
        revisionDateTime = LocalDateTime.parse("2026-06-26T11:00:00"),
      )

      // Session 3: Add Wednesday PM - exclusion entity is MODIFIED
      val session3WednesdayPM = auditRow(
        revision = 3,
        dayOfWeek = WEDNESDAY,
        timeSlot = PM,
        exclusionRevisionType = MODIFIED,
        exclusionDaysOfWeekRevisionType = ADDED,
        username = "USER3",
        revisionDateTime = LocalDateTime.parse("2026-06-27T14:00:00"),
      )

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns
        listOf(session1MondayAM, session2TuesdayAM, session3WednesdayPM)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revision = 3, dayOfWeek = WEDNESDAY, timeSlots = listOf(PM), updatedBy = "USER3", updatedDateTime = LocalDateTime.parse("2026-06-27T14:00:00")),
        exclusionRevision(revision = 2, dayOfWeek = TUESDAY, updatedBy = "USER2", updatedDateTime = LocalDateTime.parse("2026-06-26T11:00:00")),
        exclusionRevision(dayOfWeek = MONDAY),
      )

      // All should be ADDED
      assertThat(history).allMatch { it.revisionType == RevisionType.ADDED }
    }

    @Test
    fun `should return ADDED revision type when adding exclusions across weeks in separate sessions`() {
      // Session 1: Add Week 1 Monday AM
      val session1Week1MondayAM = auditRow(
        weekNumber = 1,
        dayOfWeek = MONDAY,
        exclusionRevisionType = ADDED,
        exclusionDaysOfWeekRevisionType = ADDED,
      )

      // Session 2: Add Week 2 Thursday AM - exclusion entity is MODIFIED
      val session2Week2ThursdayAM = auditRow(
        revision = 2,
        weekNumber = 2,
        dayOfWeek = THURSDAY,
        exclusionRevisionType = MODIFIED,
        exclusionDaysOfWeekRevisionType = ADDED,
        username = "USER2",
        revisionDateTime = LocalDateTime.parse("2026-06-26T11:00:00"),
      )

      // Session 3: Add Week 1 Tuesday PM - exclusion entity is MODIFIED
      val session3Week1TuesdayPM = auditRow(
        revision = 3,
        weekNumber = 1,
        dayOfWeek = TUESDAY,
        timeSlot = PM,
        exclusionRevisionType = MODIFIED,
        exclusionDaysOfWeekRevisionType = ADDED,
        username = "USER3",
        revisionDateTime = LocalDateTime.parse("2026-06-27T14:00:00"),
      )

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns
        listOf(session1Week1MondayAM, session2Week2ThursdayAM, session3Week1TuesdayPM)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revision = 3, weekNumber = 1, dayOfWeek = TUESDAY, timeSlots = listOf(PM), updatedBy = "USER3", updatedDateTime = LocalDateTime.parse("2026-06-27T14:00:00")),
        exclusionRevision(revision = 2, weekNumber = 2, dayOfWeek = THURSDAY, updatedBy = "USER2", updatedDateTime = LocalDateTime.parse("2026-06-26T11:00:00")),
        exclusionRevision(weekNumber = 1, dayOfWeek = MONDAY),
      )

      // All should be ADDED
      assertThat(history).allMatch { it.revisionType == RevisionType.ADDED }
    }

    @Test
    fun `should return REMOVED revision type when removing exclusion from existing exclusions in a later session`() {
      // Session 1: Add Monday AM and Tuesday AM
      val session1MondayAM = auditRow(
        dayOfWeek = MONDAY,
        exclusionRevisionType = ADDED,
        exclusionDaysOfWeekRevisionType = ADDED,
      )
      val session1TuesdayAM = auditRow(
        dayOfWeek = TUESDAY,
        exclusionRevisionType = ADDED,
        exclusionDaysOfWeekRevisionType = ADDED,
      )

      // Session 2: Remove Tuesday AM - exclusion entity is MODIFIED, day is DELETED
      val session2RemovedTuesdayAM = auditRow(
        revision = 2,
        dayOfWeek = TUESDAY,
        exclusionRevisionType = MODIFIED,
        exclusionDaysOfWeekRevisionType = DELETED,
        username = "USER2",
        revisionDateTime = LocalDateTime.parse("2026-06-26T11:00:00"),
      )

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns
        listOf(session1MondayAM, session1TuesdayAM, session2RemovedTuesdayAM)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revision = 2, dayOfWeek = TUESDAY, revisionType = REMOVED, updatedBy = "USER2", updatedDateTime = LocalDateTime.parse("2026-06-26T11:00:00")),
        exclusionRevision(dayOfWeek = MONDAY),
        exclusionRevision(dayOfWeek = TUESDAY),
      )
    }

    @Test
    fun `should return correct revision types for a mix of adds and removes across sessions`() {
      // Session 1: Add Monday AM
      val session1MondayAM = auditRow(
        dayOfWeek = MONDAY,
        exclusionRevisionType = ADDED,
        exclusionDaysOfWeekRevisionType = ADDED,
      )

      // Session 2: Add Tuesday AM - exclusion entity is MODIFIED, day is ADDED
      val session2TuesdayAM = auditRow(
        revision = 2,
        dayOfWeek = TUESDAY,
        exclusionRevisionType = MODIFIED,
        exclusionDaysOfWeekRevisionType = ADDED,
        username = "USER2",
        revisionDateTime = LocalDateTime.parse("2026-06-26T11:00:00"),
      )

      // Session 3: Remove Monday AM and add Wednesday PM - exclusion entity is MODIFIED
      val session3RemovedMondayAM = auditRow(
        revision = 3,
        dayOfWeek = MONDAY,
        exclusionRevisionType = MODIFIED,
        exclusionDaysOfWeekRevisionType = DELETED,
        username = "USER3",
        revisionDateTime = LocalDateTime.parse("2026-06-27T14:00:00"),
      )
      val session3AddedWednesdayPM = auditRow(
        revision = 3,
        dayOfWeek = WEDNESDAY,
        timeSlot = PM,
        exclusionRevisionType = MODIFIED,
        exclusionDaysOfWeekRevisionType = ADDED,
        username = "USER3",
        revisionDateTime = LocalDateTime.parse("2026-06-27T14:00:00"),
      )

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns
        listOf(session1MondayAM, session2TuesdayAM, session3RemovedMondayAM, session3AddedWednesdayPM)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revision = 3, dayOfWeek = MONDAY, revisionType = REMOVED, updatedBy = "USER3", updatedDateTime = LocalDateTime.parse("2026-06-27T14:00:00")),
        exclusionRevision(revision = 3, dayOfWeek = WEDNESDAY, timeSlots = listOf(PM), updatedBy = "USER3", updatedDateTime = LocalDateTime.parse("2026-06-27T14:00:00")),
        exclusionRevision(revision = 2, dayOfWeek = TUESDAY, updatedBy = "USER2", updatedDateTime = LocalDateTime.parse("2026-06-26T11:00:00")),
        exclusionRevision(dayOfWeek = MONDAY),
      )
    }

    @Test
    fun `should return correct revision types for adds and removes across weeks in separate sessions`() {
      // Session 1: Add Week 1 Monday AM and Week 2 Thursday AM
      val session1Week1MondayAM = auditRow(
        weekNumber = 1,
        dayOfWeek = MONDAY,
        exclusionRevisionType = ADDED,
        exclusionDaysOfWeekRevisionType = ADDED,
      )
      val session1Week2ThursdayAM = auditRow(
        weekNumber = 2,
        dayOfWeek = THURSDAY,
        exclusionRevisionType = ADDED,
        exclusionDaysOfWeekRevisionType = ADDED,
      )

      // Session 2: Remove Week 2 Thursday AM, add Week 1 Tuesday PM - exclusion entity is MODIFIED
      val session2RemovedWeek2ThursdayAM = auditRow(
        revision = 2,
        weekNumber = 2,
        dayOfWeek = THURSDAY,
        exclusionRevisionType = MODIFIED,
        exclusionDaysOfWeekRevisionType = DELETED,
        username = "USER2",
        revisionDateTime = LocalDateTime.parse("2026-06-26T11:00:00"),
      )
      val session2AddedWeek1TuesdayPM = auditRow(
        revision = 2,
        weekNumber = 1,
        dayOfWeek = TUESDAY,
        timeSlot = PM,
        exclusionRevisionType = MODIFIED,
        exclusionDaysOfWeekRevisionType = ADDED,
        username = "USER2",
        revisionDateTime = LocalDateTime.parse("2026-06-26T11:00:00"),
      )

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns
        listOf(session1Week1MondayAM, session1Week2ThursdayAM, session2RemovedWeek2ThursdayAM, session2AddedWeek1TuesdayPM)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revision = 2, weekNumber = 1, dayOfWeek = TUESDAY, timeSlots = listOf(PM), updatedBy = "USER2", updatedDateTime = LocalDateTime.parse("2026-06-26T11:00:00")),
        exclusionRevision(revision = 2, weekNumber = 2, dayOfWeek = THURSDAY, revisionType = REMOVED, updatedBy = "USER2", updatedDateTime = LocalDateTime.parse("2026-06-26T11:00:00")),
        exclusionRevision(weekNumber = 1, dayOfWeek = MONDAY),
        exclusionRevision(weekNumber = 2, dayOfWeek = THURSDAY),
      )
    }

    @Test
    fun `should sort by revision date time when newer revision has a lower number due to multi-instance sequence allocation`() {
      // Simulates a multi-pod deployment where Pod B pre-allocated higher revision numbers
      // Pod B handled the first request (rev 51), then Pod A handled the next request (rev 6)
      val olderDateTime = LocalDateTime.parse("2026-06-25T10:00:00")
      val newerDateTime = LocalDateTime.parse("2026-06-26T14:00:00")

      val olderRevisionHigherNumber = auditRow(
        revision = 51,
        dayOfWeek = MONDAY,
        exclusionRevisionType = ADDED,
        exclusionDaysOfWeekRevisionType = ADDED,
        username = "USER1",
        revisionDateTime = olderDateTime,
      )

      val newerRevisionLowerNumber = auditRow(
        revision = 6,
        dayOfWeek = TUESDAY,
        exclusionRevisionType = MODIFIED,
        exclusionDaysOfWeekRevisionType = ADDED,
        username = "USER2",
        revisionDateTime = newerDateTime,
      )

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns
        listOf(olderRevisionHigherNumber, newerRevisionLowerNumber)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revision = 6, dayOfWeek = TUESDAY, updatedBy = "USER2", updatedDateTime = newerDateTime),
        exclusionRevision(revision = 51, dayOfWeek = MONDAY, updatedBy = "USER1", updatedDateTime = olderDateTime),
      )
    }
  }

  /**
   * An ignored change is only where an exclusion is removed and then added back in the same revision,
   * in which case both revisions are ignored.
   */
  @Nested
  @DisplayName("Check when remove and add changes should be ignored")
  inner class IgnoredRevisionChanges {
    @Test
    fun `should exclude any changes where the exclusion was removed and added back in the same revision`() {
      val removedMondayAM = auditRow(exclusionRevisionType = MODIFIED, exclusionDaysOfWeekRevisionType = DELETED)
      val addedMondayAM = auditRow()
      val revision2DateTime = LocalDateTime.parse("2026-06-26T10:15:30")
      val addedExclusionMondayAM = auditRow(revision = 2, revisionDateTime = revision2DateTime)
      val addedExclusionMondayPM = auditRow(revision = 2, timeSlot = PM, revisionDateTime = revision2DateTime)
      val addedExclusionTuesdayAM = auditRow(revision = 2, dayOfWeek = TUESDAY, revisionDateTime = revision2DateTime)

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns
        listOf(removedMondayAM, addedExclusionMondayAM, addedMondayAM, addedExclusionTuesdayAM, addedExclusionMondayPM)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revision = 2, timeSlots = listOf(AM, PM), updatedDateTime = revision2DateTime),
        exclusionRevision(revision = 2, dayOfWeek = TUESDAY, updatedDateTime = revision2DateTime),
      )
    }

    @Test
    fun `should include any changes where the exclusion was removed and added in a different revision`() {
      val removedMondayAM = auditRow(exclusionRevisionType = MODIFIED, exclusionDaysOfWeekRevisionType = DELETED)
      val removedMondayPM = auditRow(timeSlot = PM, exclusionRevisionType = DELETED, exclusionDaysOfWeekRevisionType = DELETED)
      val revision2DateTime = LocalDateTime.parse("2026-06-26T10:15:30")
      val addedMondayAM = auditRow(revision = 2, revisionDateTime = revision2DateTime)
      val addedMondayPM = auditRow(revision = 2, timeSlot = PM, revisionDateTime = revision2DateTime)
      val addedTuesdayAM = auditRow(revision = 2, dayOfWeek = TUESDAY, revisionDateTime = revision2DateTime)

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns listOf(removedMondayAM, removedMondayPM, addedMondayAM, addedTuesdayAM, addedMondayPM)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revision = 2, timeSlots = listOf(AM, PM), updatedDateTime = revision2DateTime),
        exclusionRevision(revision = 2, dayOfWeek = TUESDAY, updatedDateTime = revision2DateTime),
        exclusionRevision(revisionType = REMOVED, timeSlots = listOf(AM, PM)),
      )
    }

    @Test
    fun `should include any changes where the week number is different`() {
      val removed = auditRow(exclusionRevisionType = MODIFIED, exclusionDaysOfWeekRevisionType = DELETED)
      val added = auditRow(weekNumber = 2)

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns listOf(removed, added)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revisionType = REMOVED),
        exclusionRevision(weekNumber = 2),
      )
    }

    @Test
    fun `should include any changes where day of week is different`() {
      val removed = auditRow(exclusionRevisionType = MODIFIED, exclusionDaysOfWeekRevisionType = DELETED)
      val added = auditRow(dayOfWeek = TUESDAY)

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns listOf(removed, added)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revisionType = REMOVED),
        exclusionRevision(dayOfWeek = TUESDAY),
      )
    }

    @Test
    fun `should include any changes where time slot is different`() {
      val removed = auditRow(exclusionRevisionType = MODIFIED, exclusionDaysOfWeekRevisionType = DELETED)
      val added = auditRow(timeSlot = ED)

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns listOf(removed, added)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revisionType = REMOVED),
        exclusionRevision(timeSlots = listOf(ED)),
      )
    }
  }

  private fun auditRow(
    weekNumber: Int = 1,
    timeSlot: TimeSlot = AM,
    dayOfWeek: DayOfWeek = MONDAY,
    revision: Long = 1,
    exclusionRevisionType: Int = ADDED,
    exclusionDaysOfWeekRevisionType: Int = ADDED,
    username: String = "USER1",
    revisionDateTime: LocalDateTime = LocalDateTime.parse("2026-06-25T10:15:30"),
  ): ExclusionHistoryAuditRow = object : ExclusionHistoryAuditRow {
    override val weekNumber = weekNumber
    override val timeSlot = timeSlot
    override val dayOfWeek = dayOfWeek
    override val revision = revision
    override val exclusionRevisionType = exclusionRevisionType
    override val exclusionDaysOfWeekRevisionType = exclusionDaysOfWeekRevisionType
    override val username = username
    override val revisionDateTime = revisionDateTime
  }
}

internal fun exclusionRevision(
  weekNumber: Int = 1,
  timeSlots: List<TimeSlot> = listOf(AM),
  dayOfWeek: DayOfWeek = MONDAY,
  revisionType: RevisionType = RevisionType.ADDED,
  revision: Long = 1,
  updatedBy: String = "USER1",
  updatedDateTime: LocalDateTime = LocalDateTime.parse("2026-06-25T10:15:30"),
) = ExclusionRevision(
  weekNumber = weekNumber,
  timeSlots = timeSlots,
  dayOfWeek = dayOfWeek,
  revisionType = revisionType,
  revision = revision,
  updatedBy = updatedBy,
  updatedDateTime = updatedDateTime,
)
