package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import java.time.LocalDate

@Service
class ActivityScheduleService(private val repository: ActivityScheduleRepository) {

  // TODO this needs to go deeper in terms of filtering by the scheduled instances
  fun getActivitySchedulesByPrisonCode(prisonCode: String, date: LocalDate, timeSlot: TimeSlot?) =
    transform(repository.findAllByActivity_PrisonCode(prisonCode))
}

enum class TimeSlot {
  AM, PM, ED
}
