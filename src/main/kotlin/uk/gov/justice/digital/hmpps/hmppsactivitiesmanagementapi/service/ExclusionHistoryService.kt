package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.hibernate.envers.RevisionType as EnversRevisionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ExclusionRevision
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RevisionType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ExclusionHistoryAuditRow
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ExclusionRepository
import java.time.DayOfWeek

@Service
@Transactional(readOnly = true)
class ExclusionHistoryService(private val exclusionRepository: ExclusionRepository) {
    fun findHistory(allocation: Allocation): List<ExclusionRevision> =
        exclusionRepository.findHistoryByAllocationId(allocation.allocationId)
            .withoutUnchangedSlotRevisions()
            .toExclusionHistories()
            .sortedWith(EXCLUSION_HISTORY_ORDERING)

    // If a slot has not changed, Envers records it as deleted and re-added in the same revision, so remove it from results.
    private fun List<ExclusionHistoryAuditRow>.withoutUnchangedSlotRevisions(): List<ExclusionHistoryAuditRow> {
        val unchangedSlotRevisionKeys = findUnchangedSlotRevisionKeys()

        return filterNot { it.slotRevisionKey in unchangedSlotRevisionKeys }
    }

    private fun List<ExclusionHistoryAuditRow>.findUnchangedSlotRevisionKeys(): Set<ExclusionSlotRevisionKey> =
        groupBy { it.slotRevisionKey }
            .filterValues { rows -> rows.wasRemovedAndReAddedInSameRevision() }
            .keys

    private fun List<ExclusionHistoryAuditRow>.toExclusionHistories(): List<ExclusionRevision> =
        groupBy { it.historyGroupKey }
            .map { (_, historyRows) -> historyRows.toExclusionHistory() }

    private fun List<ExclusionHistoryAuditRow>.toExclusionHistory(): ExclusionRevision {
        val historyRows = this
        val firstAuditRow = historyRows.first()

        return ExclusionRevision(
            weekNumber = firstAuditRow.weekNumber,
            timeSlots = historyRows.toTimeSlots(),
            dayOfWeek = firstAuditRow.dayOfWeek,
            revisionType = firstAuditRow.toRevisionType(),
            revision = firstAuditRow.revision,
            updatedBy = firstAuditRow.username,
            updatedDateTime = firstAuditRow.revisionDateTime,
        )
    }

    private fun List<ExclusionHistoryAuditRow>.toTimeSlots(): List<TimeSlot> =
        map { it.timeSlot }.sortedBy { it.ordinal }

    private fun List<ExclusionHistoryAuditRow>.wasRemovedAndReAddedInSameRevision(): Boolean =
        any { it.exclusionDaysOfWeekRevisionType == DELETED_REVISION_TYPE } &&
                any { it.exclusionDaysOfWeekRevisionType == ADDED_REVISION_TYPE }

    private fun ExclusionHistoryAuditRow.toRevisionType(): RevisionType =
        if (exclusionRevisionType == ADDED_REVISION_TYPE) RevisionType.ADDED else RevisionType.REMOVED

    private val ExclusionHistoryAuditRow.slotRevisionKey
        get() = ExclusionSlotRevisionKey(
            revision = revision,
            weekNumber = weekNumber,
            timeSlot = timeSlot,
            dayOfWeek = dayOfWeek,
        )

    private val ExclusionHistoryAuditRow.historyGroupKey
        get() = ExclusionHistoryGroupKey(
            revision = revision,
            weekNumber = weekNumber,
            dayOfWeek = dayOfWeek,
            exclusionDaysOfWeekRevisionType = exclusionDaysOfWeekRevisionType,
        )

    private data class ExclusionSlotRevisionKey(
        val revision: Long,
        val weekNumber: Int,
        val timeSlot: TimeSlot,
        val dayOfWeek: DayOfWeek,
    )

    private data class ExclusionHistoryGroupKey(
        val revision: Long,
        val weekNumber: Int,
        val dayOfWeek: DayOfWeek,
        val exclusionDaysOfWeekRevisionType: Int,
    )

    private companion object {
        private val ADDED_REVISION_TYPE = EnversRevisionType.ADD.ordinal
        private val DELETED_REVISION_TYPE = EnversRevisionType.DEL.ordinal

        private val EXCLUSION_HISTORY_ORDERING =
            compareByDescending<ExclusionRevision> { it.revision }
                .thenBy { it.weekNumber }
                .thenBy { it.dayOfWeek }
    }
}
