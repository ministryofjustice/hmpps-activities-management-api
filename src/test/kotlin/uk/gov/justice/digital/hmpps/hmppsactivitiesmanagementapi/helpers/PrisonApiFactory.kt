package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CaseLoad
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location

fun userCaseLoads(prisonCode: String) =
  listOf(CaseLoad(caseLoadId = prisonCode, description = "Prison Description", type = CaseLoad.Type.INST, currentlyActive = true))

fun appointmentLocation(locationId: Long, prisonCode: String) =
  Location(locationId = locationId, locationType = "APP", description = "Test Appointment Location", locationUsage = "APP", agencyId = prisonCode, currentOccupancy = 2)
