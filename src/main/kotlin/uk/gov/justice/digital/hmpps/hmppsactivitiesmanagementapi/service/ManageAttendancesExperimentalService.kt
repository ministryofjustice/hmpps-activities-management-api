package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceCreate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceCreateRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class ManageAttendancesExperimentalService(
  private val attendanceCreateRepository: AttendanceCreateRepository,
  private val activityRepository: ActivityRepository,
  private val allocationRepository: AllocationRepository,
  private val scheduledInstanceRepository: ScheduledInstanceRepository,
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
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

    //find possible attendance records to create
    val possibleRecords = attendanceCreateRepository.findBy(prisonCode, date)
    log.info("possibleRecords: ${possibleRecords.size}")

    //retrieve scheduled instances
    val scheduledInstanceMap = mutableMapOf<Long, ScheduledInstance>()

    val scheduledInstanceNumbers = possibleRecords.map { it.scheduledInstanceId }.distinct()
    scheduledInstanceRepository.findAllById(scheduledInstanceNumbers).forEach { scheduledInstance ->
      scheduledInstanceMap[scheduledInstance.scheduledInstanceId] = scheduledInstance
    }

    //retrieve prisoner incentive levels
    val prisonerNumbers = possibleRecords.map { it.prisonerNumber }.distinct()

    prisonerNumbers.chunked(999).forEach { prisoners ->
      val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbersMap(prisoners)
      prisonerMap.forEach { (prisonerNumber, prisoner) -> prisonerIncentiveLevelCodeMap[prisonerNumber] = prisoner?.currentIncentive?.level?.code }
    }

    val attendancesList = mutableListOf<Attendance>()

    possibleRecords.forEach {
      attendanceCreate ->

      var canAttend = true

      if(attendanceCreate.possibleExclusion) {
        val allocation = allocationRepository.findById(attendanceCreate.allocationId).orElseThrow { EntityNotFoundException("Allocation ${attendanceCreate.allocationId} not found") }
        allocation?.let { canAttend = allocation.canAttendOn(date, attendanceCreate.timeSlot) }
      // FIXME incorrect for G4732GK. Excluded thurs? is it before the call rather than direct on allocation
      }
      else {
        val endDate = when {
          attendanceCreate.allocEnd != null -> attendanceCreate.allocEnd
          //FIXME planned dealloc logic or from view
          attendanceCreate.scheduleEnd != null -> attendanceCreate.scheduleEnd
          else -> null
        }
        canAttend = date.between(attendanceCreate.allocStart, endDate )
      }

      if(canAttend) {
        val scheduledInstance = scheduledInstanceMap.get(attendanceCreate.scheduledInstanceId)!!
        val incentiveLevelCode = prisonerIncentiveLevelCodeMap.get(attendanceCreate.prisonerNumber)
        // log.info("prisonNumber: ${attendanceCreate.prisonerNumber}" )
        createAttendance(scheduledInstance, attendanceCreate, incentiveLevelCode)?.let {
          attendancesList.add(it)
          // log.info("adding ${it.prisonerNumber}")
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
   * and session status. If there is already an attendance for this person at this session
   * the function returns a null, otherwise it returns the Attendance entity to create.
   */
  private fun createAttendance(
    instance: ScheduledInstance,
    attendanceCreate: AttendanceCreate,
    incentiveLevelCode: String?,
  ): Attendance? {
    val prisonerNumber = "x" + attendanceCreate.prisonerNumber.drop(1)
     if (!attendanceAlreadyExistsFor(instance, prisonerNumber)) {
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
        val activity = activityRepository.findById(attendanceCreate.activityId).orElseThrow { EntityNotFoundException("Activity $attendanceCreate.activityId not found") }
        // log.info("ppb: ${attendanceCreate.prisonPayBandId}")
        val payBand = attendanceCreate.prisonPayBandId?.let { attendanceCreateRepository.findPayBandById(attendanceCreate.prisonPayBandId) }
        payAmount = payBand?.let { incentiveLevelCode?.let { activity.activityPayFor(payBand, incentiveLevelCode)?.rate }} ?: 0
        // payAmount = incentiveLevelCode?.let { allocation.allocationPay(incentiveLevelCode)?.rate } ?: 0
      }.also { attendance ->
        return attendance
      }
    }
    return null
  }

  private fun suspendedAttendance(instance: ScheduledInstance, prisonerNumber: String ) = Attendance(
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

  private fun attendanceAlreadyExistsFor(instance: ScheduledInstance, prisonerNumber: String) =
    attendanceRepository.existsAttendanceByScheduledInstanceAndPrisonerNumber(instance, prisonerNumber)

  fun saveAttendances(attendances: List<Attendance>, description: String): List<Attendance> {
    ManageAttendancesExperimentalService.log.info("Committing ${attendances.size} attendances for ${description}")
    return attendanceRepository.saveAllAndFlush(attendances)
  }

  fun sendCreatedEvent(newAttendance: Attendance) {
    ManageAttendancesExperimentalService.log.info("Sending sync event for attendance ID ${newAttendance.attendanceId} ${newAttendance.prisonerNumber} ${newAttendance.scheduledInstance.activitySchedule.description}")
    // outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, newAttendance.attendanceId)
  }
}
