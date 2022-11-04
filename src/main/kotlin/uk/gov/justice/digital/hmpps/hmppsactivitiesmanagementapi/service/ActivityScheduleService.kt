package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule as EntityActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule

@Service
class ActivityScheduleService(private val repository: ActivityScheduleRepository) {

  fun getScheduledInternalLocations(
    prisonCode: String,
    date: LocalDate,
    timeSlot: TimeSlot?
  ): List<InternalLocation> =
    transform(schedulesMatching(prisonCode, date, timeSlot)).mapNotNull { it.internalLocation }.distinct()

  fun getActivitySchedulesByPrisonCode(
    prisonCode: String,
    date: LocalDate,
    timeSlot: TimeSlot? = null,
    locationId: Long? = null
  ): List<ModelActivitySchedule> = transform(schedulesMatching(prisonCode, date, timeSlot, locationId))

  private fun schedulesMatching(
    prisonCode: String,
    date: LocalDate,
    timeSlot: TimeSlot? = null,
    locationId: Long? = null
  ): List<EntityActivitySchedule> {
    // TODO consider pushing some/all of the filtering logic into a repository query (perhaps using a JPA Specification)
    val filteredInstances = repository.findAllByActivity_PrisonCode(prisonCode)
      .selectSchedulesAtLocation(locationId)
      .selectSchedulesWithActiveActivitiesOn(date)
      .flatMap { it.instances }
      .selectInstancesRunningOn(date, timeSlot)

    return filteredInstances
      .groupBy { it.activitySchedule }
      .map { (schedule, instances) -> schedule.copy(instances = instances.toMutableList()) }
  }

  private fun List<ActivitySchedule>.selectSchedulesAtLocation(locationId: Long?) =
    filter { locationId == null || it.internalLocationId == locationId.toInt() }

  private fun List<ActivitySchedule>.selectSchedulesWithActiveActivitiesOn(date: LocalDate) =
    filter { it.activity.isActiveOn(date) }

  private fun List<ScheduledInstance>.selectInstancesRunningOn(date: LocalDate, timeSlot: TimeSlot?) =
    filter { it.isRunningOn(date) && (timeSlot == null || it.timeSlot() == timeSlot) }
}
