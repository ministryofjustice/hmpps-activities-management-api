package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PlannedSuspension as EntityPlannedSuspension

class PlannedSuspensionTest {

  private val now = TimeSource.now()
  private val plannedSuspensionNoEndOrCaseNote = EntityPlannedSuspension(
    allocation = allocation(),
    plannedStartDate = TimeSource.today(),
    plannedBy = "Unit test",
    plannedAt = now,
  )

  @Test
  fun `check planned suspension with no end or case note to model transformation`() {
    plannedSuspensionNoEndOrCaseNote.toModel() isEqualTo PlannedSuspension(
      plannedStartDate = TimeSource.today(),
      plannedEndDate = null,
      caseNoteId = null,
      plannedBy = "Unit test",
      plannedAt = now,
    )
  }

  @Test
  fun `check planned suspension with no case note to model transformation`() {
    val plannedSuspensionNoCaseNote = plannedSuspensionNoEndOrCaseNote.copy(plannedEndDate = TimeSource.tomorrow())

    plannedSuspensionNoCaseNote.toModel() isEqualTo PlannedSuspension(
      plannedStartDate = TimeSource.today(),
      plannedEndDate = TimeSource.tomorrow(),
      caseNoteId = null,
      plannedBy = "Unit test",
      plannedAt = now,
    )
  }

  @Test
  fun `check planned suspension with no end to model transformation`() {
    val plannedSuspensionNoEnd = plannedSuspensionNoEndOrCaseNote.copy(caseNoteId = 56789)

    plannedSuspensionNoEnd.toModel() isEqualTo PlannedSuspension(
      plannedStartDate = TimeSource.today(),
      plannedEndDate = null,
      caseNoteId = 56789,
      plannedBy = "Unit test",
      plannedAt = now,
    )
  }
}
