package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import java.time.LocalDate
import java.util.Optional

interface ActivityRepositoryCustom {
  fun getLimited(id: Long, earliestScheduledStartDate: LocalDate? = null): Optional<Activity>
}
