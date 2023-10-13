package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

@Service
@Transactional
class ManageAttendancesService(
  private val scheduledInstanceRepository: ScheduledInstanceRepository,
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val outboundEventsService: OutboundEventsService,
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient,
  private val transactionHandler: TransactionHandler,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun attendances(operation: AttendanceOperation) {
    when (operation) {
      AttendanceOperation.CREATE -> createAttendanceRecordsForToday()
      AttendanceOperation.EXPIRE -> expireUnmarkedAttendanceRecordsOneDayAfterTheirSession()
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  fun createAttendanceRecordsForToday() {
    LocalDate.now().let { today ->
      log.info("Creating attendance records for: $today")

      scheduledInstanceRepository.findAllBySessionDate(today)
        .andAttendanceRequired()
        .forEach { instance ->
          val attendancesForInstance = mutableListOf<Attendance>()

          // Get the details of prisoners due to attend the session
          val allocations = instance.getInFlightAllocations()
          val prisonerNumbers = allocations.map { it.prisonerNumber }
          val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbers(prisonerNumbers)
            .block()!!
            .associateBy { it.prisonerNumber }

          // Create these attendances - it will not duplicate if one already exists, so safe to re-run
          allocations.forEach { allocation ->
            createAttendance(instance, allocation, prisonerMap[allocation.prisonerNumber])
              ?.let { attendancesForInstance.add(it) }
          }

          if (attendancesForInstance.isNotEmpty()) {
            // Save the new attendances in a transaction
            runCatching {
              transactionHandler.newSpringTransaction {
                attendanceRepository.saveAllAndFlush(attendancesForInstance)
              }.onEach { saved ->
                // Send a sync event for each attendance row comitted
                outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, saved.attendanceId)
              }
            }
          }
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
    prisonerDetails: Prisoner?,
  ): Attendance? {
    if (!attendanceAlreadyExistsFor(instance, allocation)) {
      when {
        // Suspended prisoners produce pre-marked and unpaid suspended attendances
        allocation.status(PrisonerStatus.AUTO_SUSPENDED, PrisonerStatus.SUSPENDED) -> {
          suspendedAttendance(instance, allocation)
        }

        // Cancelled instances produce pre-marked cancelled and paid attendances
        instance.cancelled -> {
          cancelledAttendance(instance, allocation)
        }

        // By default, create an unmarked, waiting attendance
        else -> {
          waitingAttendance(instance, allocation)
        }
      }.apply {
        // Calculate what we think the pay rate should be at the prisoner's current incentive level
        val incentiveLevelCode = prisonerDetails?.currentIncentive?.level?.code
        payAmount = incentiveLevelCode ?.let { allocation.allocationPay(incentiveLevelCode)?.rate } ?: 0
      }.also { attendance ->
        return attendance
      }
    }
    return null
  }

  private fun suspendedAttendance(instance: ScheduledInstance, allocation: Allocation) = Attendance(
    scheduledInstance = instance,
    prisonerNumber = allocation.prisonerNumber,
    attendanceReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED),
    issuePayment = false,
    status = AttendanceStatus.COMPLETED,
    recordedTime = LocalDateTime.now(),
    recordedBy = ServiceName.SERVICE_NAME.value,
  )

  private fun cancelledAttendance(instance: ScheduledInstance, allocation: Allocation) = Attendance(
    scheduledInstance = instance,
    prisonerNumber = allocation.prisonerNumber,
    issuePayment = true,
    status = AttendanceStatus.COMPLETED,
    attendanceReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED),
    recordedTime = LocalDateTime.now(),
    recordedBy = ServiceName.SERVICE_NAME.value,
  )

  private fun waitingAttendance(instance: ScheduledInstance, allocation: Allocation) = Attendance(
    scheduledInstance = instance,
    prisonerNumber = allocation.prisonerNumber,
  )

  private fun attendanceAlreadyExistsFor(instance: ScheduledInstance, allocation: Allocation) =
    attendanceRepository.existsAttendanceByScheduledInstanceAndPrisonerNumber(instance, allocation.prisonerNumber)

  private fun List<ScheduledInstance>.andAttendanceRequired() = filter { it.attendanceRequired() }

  private fun ScheduledInstance.getInFlightAllocations() =
    activitySchedule.allocations().filterNot { it.status(PrisonerStatus.PENDING, PrisonerStatus.ENDED) }

  /**
   * This makes no local changes - it ONLY fires sync events to replicate the NOMIS behaviour
   * which expires attendances at the end of the day and sets the internal movement status to 'EXP'.
   */
  private fun expireUnmarkedAttendanceRecordsOneDayAfterTheirSession() {
    log.info("Expiring WAITING attendances from yesterday.")

    LocalDate.now().minusDays(1).let { yesterday ->
      val counter = AtomicInteger(0)
      forEachRolledOutPrison { prison ->
        attendanceRepository.findWaitingAttendancesOnDate(prison.code, yesterday)
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

  private fun forEachRolledOutPrison(block: (RolloutPrison) -> Unit) =
    rolloutPrisonRepository.findAll().filter { it.isActivitiesRolledOut() }.forEach { block(it) }
}

enum class AttendanceOperation {
  CREATE,
  EXPIRE,
}
