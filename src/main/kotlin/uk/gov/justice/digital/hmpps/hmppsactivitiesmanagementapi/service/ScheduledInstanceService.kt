package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository

@Service
class ScheduledInstanceService(private val repository: ScheduledInstanceRepository) {

  fun getActivityScheduleInstancesByDateRange(
    prisonCode: String,
    prisonerNumber: String?,
    dateRange: LocalDateRange
  ) = transformActivityScheduleInstances(
    if (prisonerNumber == null) repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(
      prisonCode,
      dateRange.start,
      dateRange.endInclusive
    ) else repository.getActivityScheduleInstancesByPrisonerNumberAndDateRange(
      prisonCode,
      prisonerNumber,
      dateRange.start,
      dateRange.endInclusive
    )
  )
}
