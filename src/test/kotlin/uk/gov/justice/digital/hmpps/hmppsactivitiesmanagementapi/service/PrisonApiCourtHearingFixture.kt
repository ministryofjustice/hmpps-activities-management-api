package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Agency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearing as PrisonApiCourtHearing

object PrisonApiCourtHearingFixture {
  fun instance(
    id: Long = 1,
    dateTime: String = "2022-12-14T10:00",
    location: Agency = Agency(
      agencyId = "ABDSUM",
      description = "Aberdeen Sheriff's Court (abdshf)",
      longDescription = "Aberdeen Sheriff's Court (ABDSHF)",
      agencyType = Agency.AgencyType.CRT,
      active = true,
      courtType = Agency.CourtType.OTHER,
    ),
  ) = PrisonApiCourtHearing(
    id = id,
    dateTime = dateTime,
    location = location,
  )
}
