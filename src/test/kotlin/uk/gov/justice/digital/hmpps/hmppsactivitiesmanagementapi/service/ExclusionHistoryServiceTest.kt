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
    val addedWednesdayED = auditRow(revision = 2, weekNumber = 2, username = "SMITHJ", dayOfWeek = WEDNESDAY, timeSlot = ED)
    val addedTuesdayAM = auditRow(revision = 2, weekNumber = 2, username = "SMITHJ", dayOfWeek = TUESDAY)

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
      exclusionRevision(revision = 2, weekNumber = 2, dayOfWeek = TUESDAY, updatedBy = "SMITHJ"),
      exclusionRevision(revision = 2, weekNumber = 2, dayOfWeek = WEDNESDAY, timeSlots = listOf(ED), updatedBy = "SMITHJ"),
      exclusionRevision(dayOfWeek = TUESDAY, revisionType = REMOVED),
      exclusionRevision(dayOfWeek = TUESDAY, timeSlots = listOf(PM)),
    )
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
      val addedExclusionMondayAM = auditRow(revision = 2)
      val addedExclusionMondayPM = auditRow(revision = 2, timeSlot = PM)
      val addedExclusionTuesdayAM = auditRow(revision = 2, dayOfWeek = TUESDAY)

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns
        listOf(removedMondayAM, addedExclusionMondayAM, addedMondayAM, addedExclusionTuesdayAM, addedExclusionMondayPM)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revision = 2, timeSlots = listOf(AM, PM)),
        exclusionRevision(revision = 2, dayOfWeek = TUESDAY),
      )
    }

    @Test
    fun `should include any changes where the exclusion was removed and added in a different revision`() {
      val removedMondayAM = auditRow(exclusionRevisionType = MODIFIED, exclusionDaysOfWeekRevisionType = DELETED)
      val removedMondayPM = auditRow(timeSlot = PM, exclusionRevisionType = DELETED, exclusionDaysOfWeekRevisionType = DELETED)
      val addedMondayAM = auditRow(revision = 2)
      val addedMondayPM = auditRow(revision = 2, timeSlot = PM)
      val addedTuesdayAM = auditRow(revision = 2, dayOfWeek = TUESDAY)

      every { exclusionRepository.findHistoryByAllocationId(ALLOCATION_ID) } returns listOf(removedMondayAM, removedMondayPM, addedMondayAM, addedTuesdayAM, addedMondayPM)

      val history = exclusionHistoryService.findHistory(allocation)

      assertThat(history).containsExactly(
        exclusionRevision(revision = 2, timeSlots = listOf(AM, PM)),
        exclusionRevision(revision = 2, dayOfWeek = TUESDAY),
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
    revisionDateTime: LocalDateTime? = LocalDateTime.parse("2026-06-25T10:15:30"),
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
  updatedDateTime: LocalDateTime? = LocalDateTime.parse("2026-06-25T10:15:30"),
) = ExclusionRevision(
  weekNumber = weekNumber,
  timeSlots = timeSlots,
  dayOfWeek = dayOfWeek,
  revisionType = revisionType,
  revision = revision,
  updatedBy = updatedBy,
  updatedDateTime = updatedDateTime,
)
