package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearing as PrisonApiCourtHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings as PrisonApiCourtHearings

object PrisonApiCourtHearingsFixture {
  fun instance(
    hearings: List<PrisonApiCourtHearing> = listOf(PrisonApiCourtHearingFixture.instance()),
  ): PrisonApiCourtHearings = PrisonApiCourtHearings(
    hearings = hearings
  )
}
