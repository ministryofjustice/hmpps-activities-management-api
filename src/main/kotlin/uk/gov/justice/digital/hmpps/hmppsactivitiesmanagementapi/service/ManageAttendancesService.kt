package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceCreationData
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceCreationDataRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

@Service
@Transactional
class ManageAttendancesService(
  private val attendanceCreationDataRepository: AttendanceCreationDataRepository,
  private val activityRepository: ActivityRepository,
  private val allocationRepository: AllocationRepository,
  private val scheduledInstanceRepository: ScheduledInstanceRepository,
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val prisonPayBandRepository: PrisonPayBandRepository,
  private val rolloutPrisonService: RolloutPrisonService,
  private val outboundEventsService: OutboundEventsService,
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient,
  private val transactionHandler: TransactionHandler,
  private val monitoringService: MonitoringService,
  private val clock: Clock,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createAttendances(date: LocalDate, prisonCode: String) {
    require(date <= LocalDate.now(clock)) {
      "Cannot create attendance for prison '$prisonCode', date is in the future '$date'"
    }

    require(rolloutPrisonService.isActivitiesRolledOutAt(prisonCode)) {
      "Cannot create attendance for prison '$prisonCode', not rolled out"
    }

    log.info("Creating attendance records for prison '$prisonCode' on date '$date'")

    var counter = 0
    val prisonerIncentiveLevelCodeMap = mutableMapOf<String, String?>()

    // find possible attendance records to create
    val possibleRecords = attendanceCreationDataRepository.findBy(prisonCode, date)

    val scheduledInstanceMap = mutableMapOf<Long, ScheduledInstance>()
    val activityMap = mutableMapOf<Long, Activity>()
    val prisonPayBandMap = mutableMapOf<Long, PrisonPayBand>()

    if (possibleRecords.isNotEmpty()) {
      // retrieve scheduled instances
      val scheduledInstanceNumbers = possibleRecords.map { it.scheduledInstanceId }.distinct()
      scheduledInstanceRepository.findAllById(scheduledInstanceNumbers).forEach { scheduledInstance ->
        scheduledInstanceMap[scheduledInstance.scheduledInstanceId] = scheduledInstance
      }

      // retrieve activities
      val activityIds = possibleRecords.map { it.activityId }.distinct()
      activityRepository.findAllById(activityIds).forEach { activity ->
        activityMap[activity.activityId] = activity
      }

      // retrieve prison pay bands
      val prisonPayBandIds = possibleRecords.mapNotNull { it.prisonPayBandId }.distinct()
      prisonPayBandRepository.findAllById(prisonPayBandIds).forEach { prisonPayBand ->
        prisonPayBandMap[prisonPayBand.prisonPayBandId] = prisonPayBand
      }

      // retrieve prisoner incentive levels
      val prisonerNumbers = possibleRecords.map { it.prisonerNumber }.distinct()
      val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbersMap(prisonerNumbers)
      prisonerMap.forEach { (prisonerNumber, prisoner) -> prisonerIncentiveLevelCodeMap[prisonerNumber] = prisoner?.currentIncentive?.level?.code }
    }

    val attendancesList = mutableListOf<Attendance>()

    possibleRecords.forEach {
        attendanceCreationDataRecord ->

      var canAttend = true

      if (attendanceCreationDataRecord.possibleExclusion) {
        val allocation = allocationRepository.findById(attendanceCreationDataRecord.allocationId).orElseThrow { EntityNotFoundException("Allocation ${attendanceCreationDataRecord.allocationId} not found") }
        allocation?.let { canAttend = allocation.canAttendOn(date, attendanceCreationDataRecord.timeSlot) }
      } else {
        val endDate = when {
          attendanceCreationDataRecord.allocEnd != null -> attendanceCreationDataRecord.allocEnd
          attendanceCreationDataRecord.plannedDeallocationDate != null -> attendanceCreationDataRecord.plannedDeallocationDate
          attendanceCreationDataRecord.scheduleEnd != null -> attendanceCreationDataRecord.scheduleEnd
          else -> null
        }
        canAttend = date.between(attendanceCreationDataRecord.allocStart, endDate)
      }

      if (canAttend) {
        val scheduledInstance = scheduledInstanceMap.get(attendanceCreationDataRecord.scheduledInstanceId)!!
        val incentiveLevelCode = prisonerIncentiveLevelCodeMap.get(attendanceCreationDataRecord.prisonerNumber)
        val activity = activityMap.get(attendanceCreationDataRecord.activityId)!!
        val prisonPayBand = prisonPayBandMap.get(attendanceCreationDataRecord.prisonPayBandId)
        createAttendance(scheduledInstance, attendanceCreationDataRecord, incentiveLevelCode, activity, prisonPayBand)?.let {
          attendancesList.add(it)
        }
      }
    }

    attendancesList.ifNotEmpty {
      runCatching {
        // Save the attendances for this session within a new sub-transaction
        transactionHandler.newSpringTransaction {
          saveAttendances(attendancesList, "instance.activitySchedule")
        }.onEach { savedAttendance ->
          // Send a sync event for each committed attendance row
          sendCreatedEvent(savedAttendance)
        }
      }
        .onSuccess { counter += attendancesList.size }
        .onFailure {
          monitoringService.capture("Error occurred saving attendance records for prison code '$prisonCode'", it)
          log.error(
            "Error occurred saving attendance records for prison code '$prisonCode'",
            it,
          )
        }
    }

    log.info("Created '$counter' attendance records for prison '$prisonCode' on date '$date'")
  }

  fun sendCreatedEvent(newAttendance: Attendance) {
    log.info("Sending sync event for attendance ID ${newAttendance.attendanceId} ${newAttendance.prisonerNumber} ${newAttendance.scheduledInstance.activitySchedule.description}")
    outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, newAttendance.attendanceId)
  }

  fun sendDeletedEvent(deletedAttendance: Attendance, allocation: Allocation) {
    log.info("Sending prisoner attendance deleted event for bookingId ${allocation.bookingId} and scheduledInstanceId ${deletedAttendance.scheduledInstance.scheduledInstanceId}")
    outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_DELETED, allocation.bookingId, deletedAttendance.scheduledInstance.scheduledInstanceId)
  }

  fun saveAttendances(attendances: List<Attendance>, description: String): List<Attendance> {
    log.info("Committing ${attendances.size} attendances for $description")
    return attendanceRepository.saveAllAndFlush(attendances)
  }

  fun createAnyAttendancesForToday(scheduleInstanceId: Long?, allocation: Allocation): List<Attendance> {
    if (scheduleInstanceId == null) {
      return emptyList()
    }

    val nextAvailableInstance = scheduledInstanceRepository.findOrThrowNotFound(scheduleInstanceId)

    require(nextAvailableInstance.activitySchedule == allocation.activitySchedule) {
      "Allocation does not belong to same activity schedule as selected instance"
    }

    // Need to create attendances for today?
    return allocation.activitySchedule.instances().filter {
      it.sessionDate == LocalDate.now(clock) &&
        it.startTime >= nextAvailableInstance.startTime &&
        allocation.canAttendOn(date = it.sessionDate, timeSlot = it.timeSlot)
    }.mapNotNull {
      val prisonerDetails: Prisoner? = prisonerSearchApiClient.findByPrisonerNumber(allocation.prisonerNumber)
      createAttendance(it, allocation, prisonerDetails?.currentIncentive?.level?.code)
    }
  }

  fun deleteAnyAttendancesForToday(scheduleInstanceId: Long?, allocation: Allocation): List<Attendance> {
    if (scheduleInstanceId == null) {
      return emptyList()
    }

    val nextAvailableInstance = scheduledInstanceRepository.findOrThrowNotFound(scheduleInstanceId)

    require(nextAvailableInstance.activitySchedule == allocation.activitySchedule) {
      "Allocation does not belong to same activity schedule as selected instance"
    }

    return allocation.activitySchedule.instances()
      .filter { it.sessionDate == LocalDate.now(clock) && it.startTime >= nextAvailableInstance.startTime }
      .flatMap {
        it.attendances.filter { it.prisonerNumber == allocation.prisonerNumber }
          .map { it ->
            it.scheduledInstance.remove(it)
            it
          }
      }
  }

  /**
   * This function creates the appropriate type of attendance based on the prisoner allocation
   * and session status. If there is already an attendance for this person at this session
   * the function returns a null, otherwise it returns the Attendance entity to create.
   */
  private fun createAttendance(
    instance: ScheduledInstance,
    allocation: Allocation,
    incentiveLevelCode: String?,
  ): Attendance? {
    if (!attendanceAlreadyExistsFor(instance, allocation)) {
      when {
        // Suspended prisoners produce pre-marked and unpaid suspended attendances
        allocation.status(PrisonerStatus.SUSPENDED) -> {
          suspendedAttendance(instance, allocation.prisonerNumber)
        }

        allocation.status(PrisonerStatus.AUTO_SUSPENDED) -> {
          autoSuspendedAttendance(instance, allocation.prisonerNumber)
        }

        // Cancelled instances produce pre-marked cancelled and paid attendances
        instance.cancelled -> {
          cancelledAttendance(instance, allocation.prisonerNumber)
        }

        // By default, create an unmarked, waiting attendance
        else -> {
          waitingAttendance(instance, allocation.prisonerNumber)
        }
      }.apply {
        // Calculate what we think the pay rate should be at the prisoner's current incentive level
        payAmount = incentiveLevelCode?.let { allocation.allocationPay(incentiveLevelCode)?.rate } ?: 0
      }.also { attendance ->
        return attendance
      }
    }
    return null
  }

  /**
   * This function creates the appropriate type of attendance based on the prisoner allocation
   * and session status and returns the Attendance entity to create.
   */
  private fun createAttendance(
    instance: ScheduledInstance,
    attendanceCreate: AttendanceCreationData,
    incentiveLevelCode: String?,
    activity: Activity,
    prisonPayBand: PrisonPayBand?,
  ): Attendance? {
    when {
      // Suspended prisoners produce pre-marked and unpaid suspended attendances
      attendanceCreate.prisonerStatus == PrisonerStatus.SUSPENDED -> {
        suspendedAttendance(instance, attendanceCreate.prisonerNumber)
      }
      attendanceCreate.prisonerStatus == PrisonerStatus.AUTO_SUSPENDED -> {
        autoSuspendedAttendance(instance, attendanceCreate.prisonerNumber)
      }
      // Cancelled instances produce pre-marked cancelled and paid attendances
      instance.cancelled -> {
        cancelledAttendance(instance, attendanceCreate.prisonerNumber)
      }
      // By default, create an unmarked, waiting attendance
      else -> {
        waitingAttendance(instance, attendanceCreate.prisonerNumber)
      }
    }.apply {
      // Calculate what we think the pay rate should be at the prisoner's current incentive level
      payAmount = prisonPayBand?.let { incentiveLevelCode?.let { activity.activityPayFor(prisonPayBand, incentiveLevelCode)?.rate } } ?: 0
    }.also { attendance ->
      return attendance
    }
  }

  private fun suspendedAttendance(instance: ScheduledInstance, prisonerNumber: String) = Attendance(
    scheduledInstance = instance,
    prisonerNumber = prisonerNumber,
    attendanceReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED),
    initialIssuePayment = false,
    status = AttendanceStatus.COMPLETED,
    recordedTime = LocalDateTime.now(),
    recordedBy = ServiceName.SERVICE_NAME.value,
  )

  private fun autoSuspendedAttendance(instance: ScheduledInstance, prisonerNumber: String) = Attendance(
    scheduledInstance = instance,
    prisonerNumber = prisonerNumber,
    attendanceReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.AUTO_SUSPENDED),
    initialIssuePayment = false,
    status = AttendanceStatus.COMPLETED,
    recordedTime = LocalDateTime.now(),
    recordedBy = ServiceName.SERVICE_NAME.value,
  )

  private fun cancelledAttendance(instance: ScheduledInstance, prisonerNumber: String) = Attendance(
    scheduledInstance = instance,
    prisonerNumber = prisonerNumber,
    initialIssuePayment = instance.isPaid(),
    status = AttendanceStatus.COMPLETED,
    attendanceReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED),
    comment = instance.cancelledReason,
    recordedTime = instance.cancelledTime,
    recordedBy = instance.cancelledBy,
  )

  private fun waitingAttendance(instance: ScheduledInstance, prisonerNumber: String) = Attendance(
    scheduledInstance = instance,
    prisonerNumber = prisonerNumber,
  )

  private fun attendanceAlreadyExistsFor(instance: ScheduledInstance, allocation: Allocation) =
    attendanceRepository.existsAttendanceByScheduledInstanceAndPrisonerNumber(instance, allocation.prisonerNumber)

  /**
   * This makes no local changes - it ONLY fires sync events to replicate the NOMIS behaviour
   * which expires attendances at the end of the day and sets the internal movement status to 'EXP'.
   */
  fun expireUnmarkedAttendanceRecordsOneDayAfterTheirSession() {
    log.info("Expiring WAITING attendances from yesterday.")

    LocalDate.now(clock).minusDays(1).let { yesterday ->
      val counter = AtomicInteger(0)
      forEachRolledOutPrison { prison ->
        attendanceRepository.findWaitingAttendancesOnDate(prison.prisonCode, yesterday)
          .forEach { waitingAttendance ->
            runCatching {
              outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_EXPIRED, waitingAttendance.attendanceId)
            }.onFailure {
              log.error("Failed to send expire event for attendance ID ${waitingAttendance.attendanceId}", it)
            }.onSuccess {
              counter.incrementAndGet()
            }
          }
      }

      log.info("${counter.get()} attendance record(s) expired.")
    }
  }

  private fun forEachRolledOutPrison(expireAttendances: (RolloutPrisonPlan) -> Unit) =
    rolloutPrisonService.getRolloutPrisons().forEach { expireAttendances(it) }
}
