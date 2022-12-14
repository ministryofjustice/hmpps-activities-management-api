package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
import javax.persistence.EntityNotFoundException
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
    filter { it.activity.isActive(date) }

  private fun List<ScheduledInstance>.selectInstancesRunningOn(date: LocalDate, timeSlot: TimeSlot?) =
    filter { it.isRunningOn(date) && (timeSlot == null || it.timeSlot() == timeSlot) }

  fun getAllocationsBy(scheduleId: Long, activeOnly: Boolean = true): List<Allocation> {
    val today = LocalDate.now()

    return repository.findById(scheduleId).orElseThrow {
      EntityNotFoundException("$scheduleId")
    }.allocations
      .filter { !activeOnly || it.isActive(today) }
      .toModelAllocations()
  }

  fun getScheduleById(scheduleId: Long) =
    repository.findById(scheduleId).orElseThrow {
      EntityNotFoundException("$scheduleId")
    }.toModelSchedule()

  fun allocatePrisoner(prisonerAllocationRequest: PrisonerAllocationRequest) {
    val schedule = repository.findById(prisonerAllocationRequest.scheduleId).orElseThrow {
      EntityNotFoundException("${prisonerAllocationRequest.scheduleId}")
    }

    // TODO sanitise data e.g. trim/uppercase prison number, check not empty string
    // TODO call prison api client to check prisoner is active and agency/prison code is same as activity

    schedule.allocatePrisoner(prisonerAllocationRequest.prisonerNumber)

    repository.save(schedule)
  }
}
