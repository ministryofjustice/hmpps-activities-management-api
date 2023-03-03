package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformFilteredInstances
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule as EntityActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule

@Service
class ActivityScheduleService(
  private val repository: ActivityScheduleRepository,
  private val prisonApiClient: PrisonApiClient,
  private val prisonPayBandRepository: PrisonPayBandRepository,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getScheduledInternalLocations(
    prisonCode: String,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): List<InternalLocation> =
    transformFilteredInstances(schedulesMatching(prisonCode, date, timeSlot)).mapNotNull { it.internalLocation }.distinct()

  fun getActivitySchedulesByPrisonCode(
    prisonCode: String,
    date: LocalDate,
    timeSlot: TimeSlot? = null,
    locationId: Long? = null,
  ): List<ModelActivitySchedule> = transformFilteredInstances(schedulesMatching(prisonCode, date, timeSlot, locationId))

  private fun schedulesMatching(
    prisonCode: String,
    date: LocalDate,
    timeSlot: TimeSlot? = null,
    locationId: Long? = null,
  ): Map<EntityActivitySchedule, List<ScheduledInstance>> {
    // TODO consider pushing some/all of the filtering logic into a repository query (perhaps using a JPA Specification)
    val filteredInstances = repository.findAllByActivity_PrisonCode(prisonCode)
      .selectSchedulesAtLocation(locationId)
      .selectSchedulesWithActiveActivitiesOn(date)
      .flatMap { it.instances() }
      .selectInstancesRunningOn(date, timeSlot)

    return filteredInstances.groupBy { it.activitySchedule }
  }

  private fun List<ActivitySchedule>.selectSchedulesAtLocation(locationId: Long?) =
    filter { locationId == null || it.internalLocationId == locationId.toInt() }

  private fun List<ActivitySchedule>.selectSchedulesWithActiveActivitiesOn(date: LocalDate) =
    filter { it.activity.isActive(date) }

  private fun List<ScheduledInstance>.selectInstancesRunningOn(date: LocalDate, timeSlot: TimeSlot?) =
    filter { it.isRunningOn(date) && (timeSlot == null || it.timeSlot() == timeSlot) }

  fun getAllocationsBy(scheduleId: Long, activeOnly: Boolean = true) =
    LocalDate.now().let { today ->
      repository.findOrThrowNotFound(scheduleId).allocations()
        .filter { !activeOnly || it.isActive(today) }
        .toModelAllocations()
    }

  fun getScheduleById(scheduleId: Long) = repository.findOrThrowNotFound(scheduleId).toModelSchedule()

  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  fun allocatePrisoner(scheduleId: Long, request: PrisonerAllocationRequest, allocatedBy: String) {
    log.info("Allocating prisoner ${request.prisonerNumber}.")

    val schedule = repository.findOrThrowNotFound(scheduleId)

    val prisonPayBands = prisonPayBandRepository.findByPrisonCode(schedule.activity.prisonCode)
      .associateBy { it.prisonPayBandId }
      .ifEmpty { throw IllegalArgumentException("No pay bands found for prison '${schedule.activity.prisonCode}") }

    val payBand = prisonPayBands[request.payBandId!!]
      ?: throw IllegalArgumentException("Pay band not found for prison '${schedule.activity.prisonCode}'")

    val prisonerNumber = request.prisonerNumber!!.toPrisonerNumber()

    val prisonerDetails = prisonApiClient.getPrisonerDetails(prisonerNumber.toString(), false).block()
      .let { it ?: throw IllegalArgumentException("Prisoner with prisoner number $prisonerNumber not found.") }
      .failIfNotActive()
      .failIfAtDifferentPrisonTo(schedule.activity)

    schedule.allocatePrisoner(
      prisonerNumber = prisonerNumber,
      bookingId = prisonerDetails.bookingId,
      payBand = payBand,
      allocatedBy = allocatedBy,
    )

    repository.saveAndFlush(schedule)

    log.info("Allocated prisoner $prisonerNumber to activity schedule ${schedule.description}.")
  }

  private fun InmateDetail.failIfNotActive() =
    takeIf { it.activeFlag } ?: throw IllegalStateException("Prisoner ${this.offenderNo} is not active.")

  private fun InmateDetail.failIfAtDifferentPrisonTo(activity: Activity) =
    takeIf { it.agencyId == activity.prisonCode }
      ?: throw IllegalStateException("Prisoners prison code ${this.agencyId} does not match that of the activity ${activity.prisonCode}.")
}
