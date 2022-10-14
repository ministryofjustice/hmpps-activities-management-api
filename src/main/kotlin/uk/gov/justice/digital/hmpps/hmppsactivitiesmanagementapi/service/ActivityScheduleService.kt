package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule

@Service
class ActivityScheduleService(private val repository: ActivityScheduleRepository) {

  fun getActivitySchedulesByPrisonCode(
    prisonCode: String,
    date: LocalDate,
    timeSlot: TimeSlot?
  ): List<ModelActivitySchedule> {
    // TODO consider pushing some/all of the filtering logic into a repository query (perhaps using a JPA Specification)
    val filteredInstances = repository.findAllByActivity_PrisonCode(prisonCode)
      .selectSchedulesWithActiveActivitiesOn(date)
      .flatMap { it.instances }
      .selectInstancesRunningOn(date, timeSlot)

    val schedulesWithInstancesOnChosenDateAndSlot = filteredInstances
      .groupBy { it.activitySchedule }
      .map { (schedule, instances) -> schedule.copy(instances = instances.toMutableList()) }

    return transform(schedulesWithInstancesOnChosenDateAndSlot)
  }

  private fun List<ActivitySchedule>.selectSchedulesWithActiveActivitiesOn(date: LocalDate) =
    filter { it.activity.isActiveOn(date) }

  private fun List<ScheduledInstance>.selectInstancesRunningOn(date: LocalDate, timeSlot: TimeSlot?) =
    filter { it.isRunningOn(date) && (timeSlot == null || it.timeSlot() == timeSlot) }
}
