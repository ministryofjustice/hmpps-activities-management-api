package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import toPrisonerAllocatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteSubType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isActiveIn
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_NUMBER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.createAllocationTelemetryMetricsMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.createAllocationTelemetryPropertiesMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.determineEarliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformFilteredInstances
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule as EntityActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation as ModelAllocation

@Service
@Transactional(readOnly = true)
class ActivityScheduleService(
  private val repository: ActivityScheduleRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val caseNotesApiClient: CaseNotesApiClient,
  private val prisonPayBandRepository: PrisonPayBandRepository,
  private val waitingListRepository: WaitingListRepository,
  private val auditService: AuditService,
  private val telemetryClient: TelemetryClient,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getScheduledInternalLocations(
    prisonCode: String,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): List<InternalLocation> =
    transformFilteredInstances(schedulesMatching(prisonCode, date, timeSlot)).mapNotNull { it.internalLocation }
      .distinct()

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

  fun getAllocationsBy(
    scheduleId: Long,
    activeOnly: Boolean = true,
    includePrisonerSummary: Boolean = false,
    activeOn: LocalDate? = null,
  ): List<ModelAllocation> {
    val activitySchedule = repository.getActivityScheduleByIdWithFilters(
      scheduleId,
      allocationsActiveOnDate = activeOn,
    ) ?: throw EntityNotFoundException("Activity Schedule $scheduleId not found")

    return activitySchedule
      .checkCaseloadAccess()
      .allocations()
      .filter { !activeOnly || !it.status(PrisonerStatus.ENDED) }
      .toModelAllocations()
      .apply {
        if (includePrisonerSummary) {
          val prisoners =
            prisonerSearchApiClient.findByPrisonerNumbers(map { it.prisonerNumber })

          map {
            val prisoner = prisoners.find { p -> it.prisonerNumber == p.prisonerNumber }!!
            it.prisonerName = "${prisoner.firstName} ${prisoner.lastName}"
            it.cellLocation = prisoner.cellLocation
            it.earliestReleaseDate = determineEarliestReleaseDate(prisoner)
          }
        }
      }
  }

  fun getScheduleById(scheduleId: Long, earliestSessionDate: LocalDate) =
    repository.getActivityScheduleByIdWithFilters(
      activityScheduleId = scheduleId,
      earliestSessionDate = earliestSessionDate,
    )?.checkCaseloadAccess()?.toModelSchedule() ?: throw EntityNotFoundException("Activity schedule ID $scheduleId not found")

  @Transactional
  fun allocatePrisoner(scheduleId: Long, request: PrisonerAllocationRequest, allocatedBy: String) {
    log.info("Allocating prisoner ${request.prisonerNumber}.")

    require(request.startDate!! > LocalDate.now()) { "Allocation start date must be in the future" }

    transactionHandler.newSpringTransaction {
      val schedule = repository.findOrThrowNotFound(scheduleId).also {
        if (it.isPaid().not() && request.payBandId != null) throw IllegalArgumentException("Allocation cannot have a pay band when the activity '${it.activity.activityId}' is unpaid")
        if (it.isPaid() && request.payBandId == null) throw IllegalArgumentException("Allocation must have a pay band when the activity '${it.activity.activityId}' is paid")
      }

      val payBand = request.payBandId?.let {
        val prisonPayBands = prisonPayBandRepository.findByPrisonCode(schedule.activity.prisonCode)
          .associateBy { it.prisonPayBandId }
          .ifEmpty { throw IllegalArgumentException("No pay bands found for prison '${schedule.activity.prisonCode}") }

        prisonPayBands[request.payBandId]
          ?: throw IllegalArgumentException("Pay band not found for prison '${schedule.activity.prisonCode}'")
      }

      val prisonerNumber = request.prisonerNumber!!.toPrisonerNumber()

      val activePrisoner = prisonerSearchApiClient.findByPrisonerNumber(request.prisonerNumber)
        ?.also { prisoner ->
          require(prisoner.isActiveIn(schedule.activity.prisonCode)) {
            "Unable to allocate prisoner with prisoner number $prisonerNumber, prisoner is not active in prison ${schedule.activity.prisonCode}."
          }

          requireNotNull(prisoner.bookingId) {
            "Unable to allocate prisoner with prisoner number $prisonerNumber, prisoner does not have a booking id."
          }
        } ?: throw IllegalArgumentException("Unable to allocate prisoner with prisoner number $prisonerNumber to schedule $scheduleId, prisoner not found.")

      schedule.allocatePrisoner(
        prisonerNumber = prisonerNumber,
        bookingId = activePrisoner.bookingId!!.toLong(),
        payBand = payBand,
        startDate = request.startDate,
        endDate = request.endDate,
        exclusions = request.exclusions,
        allocatedBy = allocatedBy,
      ).let { allocation ->
        val maybeWaitingList = waitingListRepository.findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
          schedule.activity.prisonCode,
          request.prisonerNumber,
          schedule,
        )
          .also { waitingLists ->
            require(waitingLists.none(WaitingList::isPending)) {
              "Prisoner has a PENDING waiting list application. It must be APPROVED before they can be allocated."
            }
          }
          .filter(WaitingList::isApproved)
          .also { waitingLists ->
            require(waitingLists.size <= 1) {
              "Prisoner has more than one APPROVED waiting list application. A prisoner can only have one approved waiting list application."
            }
          }
          .singleOrNull()?.allocated(allocation)

        repository.saveAndFlush(schedule)
        auditService.logEvent(allocation.toPrisonerAllocatedEvent(maybeWaitingList?.waitingListId))
        logAllocationEvent(allocation, maybeWaitingList)
        log.info("Allocated prisoner $prisonerNumber to activity schedule ${schedule.description}.")

        allocation.allocationId
      }
    }.also { allocationId -> outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATED, allocationId) }
  }

  @Transactional
  fun deallocatePrisoners(scheduleId: Long, request: PrisonerDeallocationRequest, deallocatedBy: String) {
    log.info("Attempting to deallocate prisoners $request")

    transactionHandler.newSpringTransaction {
      repository.findOrThrowNotFound(scheduleId).run {
        request.prisonerNumbers!!.distinct()
          .map { prisonerNumber ->
            var caseNoteId: Long? = null
            if (request.caseNote != null) {
              val subType = if (request.caseNote.type == CaseNoteType.GEN) CaseNoteSubType.OSE else CaseNoteSubType.NEG_GEN
              caseNoteId = caseNotesApiClient.postCaseNote(activity.prisonCode, prisonerNumber, request.caseNote.text, request.caseNote.type, subType).caseNoteId.toLong()
            }

            deallocatePrisonerOn(
              prisonerNumber,
              request.endDate!!,
              request.reasonCode.toDeallocationReason(),
              deallocatedBy,
              caseNoteId,
            ).also { log.info("Planned deallocation of prisoner ${it.prisonerNumber} from activity schedule id ${this.activityScheduleId}") }
          }.also { repository.saveAndFlush(this) }.map { it.allocationId to it.prisonerNumber }
      }
    }.onEach { (allocationId, prisonerNumber) ->
      outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
      logDeallocationEvent(prisonerNumber)
    }
  }

  private fun String?.toDeallocationReason() =
    DeallocationReason.entries
      .filter(DeallocationReason::displayed)
      .firstOrNull { it.name == this }
      ?: throw IllegalArgumentException("Invalid deallocation reason specified '$this'")

  private fun ActivitySchedule.checkCaseloadAccess() = also { checkCaseloadAccess(activity.prisonCode) }

  private fun logAllocationEvent(allocation: Allocation, maybeWaitingList: WaitingList?) {
    val propertiesMap = allocation.createAllocationTelemetryPropertiesMap(maybeWaitingList)
    val metricsMap = allocation.createAllocationTelemetryMetricsMap(maybeWaitingList)
    telemetryClient.trackEvent(TelemetryEvent.CREATE_ALLOCATION.value, propertiesMap, metricsMap)
  }

  private fun logDeallocationEvent(prisonerNumber: String) {
    val propertiesMap = mapOf(PRISONER_NUMBER_PROPERTY_KEY to prisonerNumber)
    telemetryClient.trackEvent(TelemetryEvent.PRISONER_DEALLOCATED.value, propertiesMap)
  }
}
