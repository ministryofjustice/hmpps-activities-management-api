package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.Location.LocationType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.Location.Status
import java.time.LocalDateTime
import java.util.*

fun dpsLocation(
  id: UUID = UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc"),
  prisonId: String = MOORLAND_PRISON_CODE,
  code: String = "EDUC-ED1-ED1",
  localName: String? = "User Description",
) = Location(
  id = id,
  prisonId = prisonId,
  code = code,
  pathHierarchy = "EDUC-ED1-ED1",
  locationType = LocationType.AREA,
  permanentlyInactive = false,
  status = Status.ACTIVE,
  active = true,
  deactivatedByParent = false,
  topLevelId = UUID.fromString("999998b5-fbe4-43ae-89c4-cadbc299dd98"),
  level = 3,
  leafLevel = true,
  lastModifiedBy = "A_USER",
  lastModifiedDate = LocalDateTime.of(2024, 11, 24, 10, 33),
  key = "MDI-EDUC-ED1-ED1",
  isResidential = false,
  localName = localName,
  locked = false,
)
