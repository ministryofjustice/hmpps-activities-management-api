package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation

internal fun ActivitySchedule.allocatedPrisoner(prisonNumber: String) = allocations.allocatedPrisoner(prisonNumber)

internal fun List<Allocation>.allocatedPrisoner(prisonNumber: String) =
  firstOrNull { it.prisonerNumber.uppercase() == prisonNumber.uppercase() }
    ?: throw AssertionError("Allocated $prisonNumber not found.")
