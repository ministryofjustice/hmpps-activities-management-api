package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CaseLoad
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.UserDetail

fun userDetail(
  id: Long = 1,
  username: String = "TEST.USER",
  firstName: String = "TEST",
  lastName: String = "USER",
) = UserDetail(
  staffId = id,
  username = username,
  firstName = firstName,
  lastName = lastName,
  accountStatus = UserDetail.AccountStatus.ACTIVE,
  active = true,
)

fun userCaseLoads(prisonCode: String) =
  listOf(CaseLoad(caseLoadId = prisonCode, description = "Prison Description", type = CaseLoad.Type.INST, currentlyActive = true))

fun appointmentLocation(locationId: Long, prisonCode: String) =
  Location(locationId = locationId, locationType = "APP", description = "Test Appointment Location", locationUsage = "APP", agencyId = prisonCode, currentOccupancy = 2)

fun appointmentCategoryReferenceCode(code: String = "TEST", description: String = "Test Category") =
  ReferenceCode(
    domain = "INT_SCH_RSN",
    code = code,
    description = description,
    activeFlag = "Y",
  )
