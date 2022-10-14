package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation

internal fun ActivitySchedule.prisoner(prisonNumber: String) = allocations.prisoner(prisonNumber)

internal fun List<Allocation>.prisoner(prisonNumber: String) =
  firstOrNull { it.prisonerNumber.uppercase() == prisonNumber.uppercase() }
    ?: throw AssertionError("Allocated $prisonNumber not found.")
