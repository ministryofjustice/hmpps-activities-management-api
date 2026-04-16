package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import toPrisonerAllocatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteSubType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api.NonAssociationsApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isActiveAtPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySuitabilityCriteria
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_NUMBER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.createAllocationTelemetryMetricsMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.createAllocationTelemetryPropertiesMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.determineEarliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.hasNonAssociations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelSchedule
import java.time.LocalDate
import java.util.*
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
  private val manageAttendancesService: ManageAttendancesService,
  private val nonAssociationsApiClient: NonAssociationsApiClient,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getAllocationsBy(
    scheduleId: Long,
    activeOnly: Boolean = true,
    includePrisonerSummary: Boolean = false,
    activeOn: LocalDate? = null,
  ): List<ModelAllocation> = runBlocking {
    val activitySchedule = repository.getActivityScheduleByIdWithFilters(
      scheduleId,
      allocationsActiveOnDate = activeOn,
    ) ?: throw EntityNotFoundException("Activity Schedule $scheduleId not found")

    activitySchedule
      .checkCaseloadAccess()
      .allocations()
      .filter { !activeOnly || !it.status(PrisonerStatus.ENDED) }
      .toModelAllocations()
      .apply {
        if (includePrisonerSummary) {
          val prisonerNumbers = map { it.prisonerNumber }.distinct()

          val prisoners = async { prisonerSearchApiClient.findByPrisonerNumbersAsync(prisonerNumbers) }

          val nonAssociations = async { nonAssociationsApiClient.getNonAssociationsInvolving(activitySchedule.activity.prisonCode, prisonerNumbers) }

          map {
            val prisoner = prisoners.await().find { p -> it.prisonerNumber == p.prisonerNumber } ?: throw NullPointerException("Prisoner ${it.prisonerNumber} not found for allocation id ${it.id}")
            it.prisonerName = "${prisoner.firstName} ${prisoner.lastName}"
            it.prisonerFirstName = prisoner.firstName
            it.prisonerLastName = prisoner.lastName
            it.prisonerStatus = prisoner.status
            it.prisonerPrisonCode = prisoner.prisonId
            it.cellLocation = prisoner.cellLocation
            it.earliestReleaseDate = determineEarliestReleaseDate(prisoner)
            it.nonAssociations = nonAssociations.await()?.hasNonAssociations(it.prisonerNumber)
          }
        }
      }
  }

  fun getScheduleById(scheduleId: Long, earliestSessionDate: LocalDate, adminMode: Boolean? = false) = repository.getActivityScheduleByIdWithFilters(
    activityScheduleId = scheduleId,
    earliestSessionDate = earliestSessionDate,
  )?.checkCaseloadAccess(adminMode)?.toModelSchedule() ?: throw EntityNotFoundException("Activity schedule ID $scheduleId not found")

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  fun allocatePrisoner(scheduleId: Long, request: PrisonerAllocationRequest, allocatedBy: String, adminMode: Boolean? = false) {
    log.info("Allocating prisoner ${request.prisonerNumber}.")

    val today = LocalDate.now()

    require(request.startDate!! >= today) { "Allocation start date must not be in the past" }

    if (request.startDate == today && request.scheduleInstanceId == null) throw IllegalArgumentException("The next session must be provided when allocation start date is today")

    val activePrisoner = prisonerSearchApiClient.findByPrisonerNumber(request.prisonerNumber!!)

    transactionHandler.newSpringTransaction {
      val schedule = repository.findOrThrowNotFound(scheduleId).also {
        if (it.isPaid().not() && request.payBandId != null) throw IllegalArgumentException("Allocation cannot have a pay band when the activity '${it.activity.activityId}' is unpaid")
        if (it.isPaid() && request.payBandId == null) throw IllegalArgumentException("Allocation must have a pay band when the activity '${it.activity.activityId}' is paid")
      }

      val payBand = request.payBandId?.let {
        val prisonPayBands = prisonPayBandRepository.findByPrisonCode(schedule.activity.prisonCode)
          .associateBy { it.prisonPayBandId }
          .ifEmpty { throw IllegalArgumentException("No pay bands found for prison '${schedule.activity.prisonCode}'") }

        prisonPayBands[request.payBandId]
          ?: throw IllegalArgumentException("Pay band not found for prison '${schedule.activity.prisonCode}'")
      }

      val prisonerNumber = request.prisonerNumber.toPrisonerNumber()

      activePrisoner
        ?.also { prisoner ->
          require(prisoner.isActiveAtPrison(schedule.activity.prisonCode)) {
            "Unable to allocate prisoner with prisoner number $prisonerNumber, prisoner is not active at prison ${schedule.activity.prisonCode}."
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

        // if allocation is for instance(s) later today then need to create attendance records
        val newAttendances = manageAttendancesService.createAnyAttendancesForToday(
          allocation = allocation,
          scheduleInstanceId = request.scheduleInstanceId,
        )

        repository.saveAndFlush(schedule)

        val savedAttendances = manageAttendancesService.saveAttendances(newAttendances, schedule.description)

        auditService.logEvent(allocation.toPrisonerAllocatedEvent(maybeWaitingList?.waitingListId))
        logAllocationEvent(allocation, maybeWaitingList)
        log.info("Allocated prisoner $prisonerNumber to activity schedule ${schedule.description}.")

        allocation.allocationId to savedAttendances
      }
    }.also { (allocation, newAttendances) ->
      outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATED, allocation)

      newAttendances.forEach { manageAttendancesService.sendCreatedEvent(it) }
    }
  }

//  @Transactional not needed - transactionHandler.newSpringTransaction() handles this
  fun allocatePrisonersToSchedule(scheduleId: Long, requests: List<PrisonerAllocationRequest>, allocatedBy: String) {
    log.info("Bulk allocating ${requests.size} prisoners to schedule $scheduleId")

    val today = LocalDate.now()

    requests.forEach { request ->
      require(request.startDate!! >= today) { "Allocation start date must not be in the past" }
      if (request.startDate == today && request.scheduleInstanceId == null) {
        throw IllegalArgumentException("The next session must be provided when allocation start date is today")
      }
    }

    // batch fetch all prisoners
    val prisonerNumbers = requests.map { it.prisonerNumber!! }.distinct()
    val prisonersMap = runBlocking {
      prisonerSearchApiClient.findByPrisonerNumbersAsync(prisonerNumbers)
        .associateBy { it.prisonerNumber }
    }

    transactionHandler.newSpringTransaction {
      val schedule = repository.findOrThrowNotFound(scheduleId)

      // batch fetch pay bands once to avoid N+1 queries
      val prisonPayBands = prisonPayBandRepository.findByPrisonCode(schedule.activity.prisonCode)
        .associateBy { it.prisonPayBandId }

      // Store allocations with their attendances and waiting list info for audit logging after saveAndFlush
      val allocationsWithAttendances = mutableListOf<Triple<Allocation, List<Attendance>, WaitingList?>>()

      requests.forEach { request ->
        log.info("Allocating prisoner ${request.prisonerNumber} to schedule $scheduleId")

        val prisonerNumber = request.prisonerNumber!!.toPrisonerNumber()

        if (schedule.isPaid().not() && request.payBandId != null) {
          throw IllegalArgumentException("Allocation cannot have a pay band when the activity '${schedule.activity.activityId}' is unpaid")
        }
        if (schedule.isPaid() && request.payBandId == null) {
          throw IllegalArgumentException("Allocation must have a pay band when the activity '${schedule.activity.activityId}' is paid")
        }

        val payBand = request.payBandId?.let {
          if (prisonPayBands.isEmpty()) {
            throw IllegalArgumentException("No pay bands found for prison '${schedule.activity.prisonCode}'")
          }
          prisonPayBands[request.payBandId]
            ?: throw IllegalArgumentException("Pay band not found for prison '${schedule.activity.prisonCode}'")
        }

        val activePrisoner = prisonersMap[request.prisonerNumber]
          ?: run {
            log.error("Prisoner not found in prisoner search results. Prisoner number: $prisonerNumber, Schedule ID: $scheduleId, PrisonersMap: ${prisonersMap.keys}")
            throw IllegalArgumentException("Unable to allocate prisoner with prisoner number $prisonerNumber to schedule $scheduleId, prisoner not found.")
          }

        require(activePrisoner.isActiveAtPrison(schedule.activity.prisonCode)) {
          "Unable to allocate prisoner with prisoner number $prisonerNumber, prisoner is not active at prison ${schedule.activity.prisonCode}."
        }

        requireNotNull(activePrisoner.bookingId) {
          "Unable to allocate prisoner with prisoner number $prisonerNumber, prisoner does not have a booking id."
        }

        schedule.allocatePrisoner(
          prisonerNumber = prisonerNumber,
          bookingId = activePrisoner.bookingId.toLong(),
          payBand = payBand,
          startDate = request.startDate!!,
          endDate = request.endDate,
          exclusions = request.exclusions,
          allocatedBy = allocatedBy,
        ).let { allocation ->
          // Find waiting list but don't associate allocation yet (allocation hasn't been saved to database and is still transient)
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
            .singleOrNull()

          // if allocations are for instance(s) later today then need to create attendance records
          val newAttendances = manageAttendancesService.createAnyAttendancesForToday(
            allocation = allocation,
            scheduleInstanceId = request.scheduleInstanceId,
          )
          allocationsWithAttendances.add(Triple(allocation, newAttendances, maybeWaitingList))
        }
      }

      repository.saveAndFlush(schedule)

      allocationsWithAttendances.map { (allocation, attendances, maybeWaitingList) ->
        val savedAttendances = manageAttendancesService.saveAttendances(attendances, schedule.description)

        maybeWaitingList?.allocated(allocation)?.also { waitingListRepository.save(it) }

        auditService.logEvent(allocation.toPrisonerAllocatedEvent(maybeWaitingList?.waitingListId))
        logAllocationEvent(allocation, maybeWaitingList)
        log.info("Allocated prisoner ${allocation.prisonerNumber} to activity schedule ${schedule.description}.")

        allocation.allocationId to savedAttendances
      }
    }.also { allocationIdAndAttendancesPairs ->
      allocationIdAndAttendancesPairs.forEach { (allocationId, newAttendances) ->
        outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATED, allocationId)

        newAttendances.forEach { manageAttendancesService.sendCreatedEvent(it) }
      }
    }

    log.info("Completed bulk allocation of ${requests.size} prisoners to schedule $scheduleId.")
  }

  fun getSuitabilityCriteria(scheduleId: Long): ActivitySuitabilityCriteria? {
    val activitySchedule = repository.findOrThrowNotFound(scheduleId)
    return activitySchedule.toModelActivitySuitabilityCriteria()
  }

  @Transactional
  fun deallocatePrisoners(scheduleId: Long, request: PrisonerDeallocationRequest, deallocatedBy: String) {
    log.info("Attempting to deallocate prisoners $request")

    transactionHandler.newSpringTransaction {
      repository.findOrThrowNotFound(scheduleId).run {
        request.prisonerNumbers!!.distinct()
          .also {
            if (request.scheduleInstanceId != null) {
              require(it.size == 1) { "Cannot deallocate sessions later today for multiple prisoners" }
            }
          }
          .map { prisonerNumber ->
            val deallocationReason = request.reasonCode!!.toDeallocationReason()
            val endDate = request.endDate!!
            var dpsCaseNoteId: UUID? = null

            if (request.caseNote != null) {
              val subType = if (request.caseNote.type == CaseNoteType.GEN) CaseNoteSubType.HIS else CaseNoteSubType.NEG_GEN

              dpsCaseNoteId = UUID.fromString(
                caseNotesApiClient.postCaseNote(
                  activity.prisonCode,
                  prisonerNumber,
                  request.caseNote.text!!,
                  request.caseNote.type!!,
                  subType,
                  "Deallocated from activity - ${deallocationReason.description} - ${activity.summary}",
                )
                  .caseNoteId,
              )
            }

            deallocatePrisonerOn(
              prisonerNumber,
              endDate,
              deallocationReason,
              deallocatedBy,
              dpsCaseNoteId,
            )
              .also { log.info("Planned deallocation of prisoner ${it.prisonerNumber} from activity schedule id ${this.activityScheduleId}") }
              .let {
                val deletedAttendances = manageAttendancesService.deleteAnyAttendancesForToday(request.scheduleInstanceId, it)
                it to deletedAttendances
              }
          }
          .also { repository.saveAndFlush(this) }
      }
    }.onEach { (allocation, deletedAttendances) ->
      outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocation.allocationId)

      // Should only happen for one prisoner due to earlier require
      deletedAttendances.forEach { manageAttendancesService.sendDeletedEvent(it, allocation) }

      logDeallocationEvent(allocation.prisonerNumber)
    }
  }

  private fun String.toDeallocationReason() = DeallocationReason.entries
    .filter(DeallocationReason::displayed)
    .firstOrNull { it.name == this }
    ?: throw IllegalArgumentException("Invalid deallocation reason specified '$this'")

  private fun ActivitySchedule.checkCaseloadAccess(adminMode: Boolean? = false) = also { if (adminMode == false) checkCaseloadAccess(activity.prisonCode) }

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
