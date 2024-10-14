package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceCreationData
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceCreationDataRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class ManageAttendancesExperimentalService(
  private val attendanceCreationDataRepository: AttendanceCreationDataRepository,
  private val activityRepository: ActivityRepository,
  private val allocationRepository: AllocationRepository,
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val prisonPayBandRepository: PrisonPayBandRepository,
  private val scheduledInstanceRepository: ScheduledInstanceRepository,
  private val outboundEventsService: OutboundEventsService,
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient,
  private val rolloutPrisonService: RolloutPrisonService,
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

    // retrieve scheduled instances
    val scheduledInstanceMap = mutableMapOf<Long, ScheduledInstance>()
    val scheduledInstanceNumbers = possibleRecords.map { it.scheduledInstanceId }.distinct()
    scheduledInstanceRepository.findAllById(scheduledInstanceNumbers).forEach { scheduledInstance ->
      scheduledInstanceMap[scheduledInstance.scheduledInstanceId] = scheduledInstance
    }

    // retrieve activities
    val activityMap = mutableMapOf<Long, Activity>()
    val activityIds = possibleRecords.map { it.activityId }.distinct()
    activityRepository.findAllById(activityIds).forEach { activity ->
      activityMap[activity.activityId] = activity
    }

    // retrieve prison pay bands
    val prisonPayBandMap = mutableMapOf<Long, PrisonPayBand>()
    val prisonPayBandIds = possibleRecords.mapNotNull { it.prisonPayBandId }.distinct()
    prisonPayBandRepository.findAllById(prisonPayBandIds).forEach { prisonPayBand ->
      prisonPayBandMap[prisonPayBand.prisonPayBandId] = prisonPayBand
    }

    // retrieve prisoner incentive levels
    val prisonerNumbers = possibleRecords.map { it.prisonerNumber }.distinct()
    val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbersMap(prisonerNumbers)
    prisonerMap.forEach { (prisonerNumber, prisoner) -> prisonerIncentiveLevelCodeMap[prisonerNumber] = prisoner?.currentIncentive?.level?.code }

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
          monitoringService.capture("Error occurred saving attendances for prison code '$prisonCode'", it)
          log.error(
            "Error occurred saving attendances for prison code '$prisonCode'",
            it,
          )
        }
    }

    log.info("Created '$counter' experimental attendance records for prison '$prisonCode' on date '$date'")
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
    val prisonerNumber = "x" + attendanceCreate.prisonerNumber.drop(1)
    when {
      // Suspended prisoners produce pre-marked and unpaid suspended attendances
      attendanceCreate.prisonerStatus == PrisonerStatus.SUSPENDED -> {
        suspendedAttendance(instance, prisonerNumber)
      }
      attendanceCreate.prisonerStatus == PrisonerStatus.AUTO_SUSPENDED -> {
        autoSuspendedAttendance(instance, prisonerNumber)
      }
      // Cancelled instances produce pre-marked cancelled and paid attendances
      instance.cancelled -> {
        cancelledAttendance(instance, prisonerNumber)
      }
      // By default, create an unmarked, waiting attendance
      else -> {
        waitingAttendance(instance, prisonerNumber)
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

  fun saveAttendances(attendances: List<Attendance>, description: String): List<Attendance> {
    log.info("Committing ${attendances.size} attendances for $description")
    return attendanceRepository.saveAllAndFlush(attendances)
  }

  fun sendCreatedEvent(newAttendance: Attendance) {
    log.info("Sending sync event for attendance ID ${newAttendance.attendanceId} ${newAttendance.prisonerNumber}")
    // outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, newAttendance.attendanceId)
  }
}
