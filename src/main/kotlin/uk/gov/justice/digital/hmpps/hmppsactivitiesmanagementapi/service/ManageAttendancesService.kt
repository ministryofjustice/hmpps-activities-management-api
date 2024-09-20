package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AttendanceReasonRepository
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

    // Schedules instance for AM might be created if next session was PM. Need thinking
    scheduledInstanceRepository.getActivityScheduleInstancesByPrisonCodeAndDateRange(
      prisonCode = prisonCode,
      startDate = date,
      endDate = date,
    )
      .forEach { instance ->
        // Get the allocations which can be attended on the supplied date and time slot for the instance
        val allocations = instance.activitySchedule.allocations().filter { it.canAttendOn(date, instance.timeSlot) }

        // Get the details of the prisoners due to attend the session
        val prisonerNumbers = allocations.map { it.prisonerNumber }
        val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbersMap(prisonerNumbers)

        // Build up a list of attendances required - it will not duplicate if one already exists, so safe to re-run
        val attendancesForInstance = allocations
          .mapNotNull { allocation ->
            createAttendance(instance, allocation, prisonerMap[allocation.prisonerNumber])
          }

        attendancesForInstance.ifNotEmpty {
          runCatching {
            // Save the attendances for this session within a new sub-transaction
            transactionHandler.newSpringTransaction {
              saveAttendances(attendancesForInstance, instance.activitySchedule)
            }.onEach { savedAttendance ->
              // Send a sync event for each committed attendance row
              sendCreatedEvent(savedAttendance)
            }
          }
            .onSuccess { counter += attendancesForInstance.size }
            .onFailure {
              monitoringService.capture("Error occurred saving attendances for prison code '$prisonCode' and instance id '${instance.scheduledInstanceId}'", it)
              log.error(
                "Error occurred saving attendances for prison code '$prisonCode' and instance id '${instance.scheduledInstanceId}'",
                it,
              )
            }
        }
      }

    log.info("Created '$counter' attendance records for prison '$prisonCode' on date '$date'")
  }

  fun sendCreatedEvent(newAttendance: Attendance) {
    log.info("Sending sync event for attendance ID ${newAttendance.attendanceId} ${newAttendance.prisonerNumber} ${newAttendance.scheduledInstance.activitySchedule.description}")
    outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, newAttendance.attendanceId)
  }

  fun sendDeletedEvent(deletedAttendance: Attendance, allocation: Allocation) {
    log.info("Sending prisoner attendance deleted event for bookingId ${deletedAttendance.attendanceId} and scheduledInstance ${allocation.bookingId}")
    outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_DELETED, allocation.bookingId, deletedAttendance.scheduledInstance.scheduledInstanceId)
  }

  fun saveAttendances(attendances: List<Attendance>, activitySchedule: ActivitySchedule): List<Attendance> {
    log.info("Committing ${attendances.size} attendances for ${activitySchedule.description}")
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
      createAttendance(it, allocation, prisonerSearchApiClient.findByPrisonerNumber(allocation.prisonerNumber))
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
    prisonerDetails: Prisoner?,
  ): Attendance? {
    if (!attendanceAlreadyExistsFor(instance, allocation)) {
      when {
        // Suspended prisoners produce pre-marked and unpaid suspended attendances
        allocation.status(PrisonerStatus.SUSPENDED) -> {
          suspendedAttendance(instance, allocation)
        }

        allocation.status(PrisonerStatus.AUTO_SUSPENDED) -> {
          autoSuspendedAttendance(instance, allocation)
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
        payAmount = incentiveLevelCode?.let { allocation.allocationPay(incentiveLevelCode)?.rate } ?: 0
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
    initialIssuePayment = false,
    status = AttendanceStatus.COMPLETED,
    recordedTime = LocalDateTime.now(),
    recordedBy = ServiceName.SERVICE_NAME.value,
  )

  private fun autoSuspendedAttendance(instance: ScheduledInstance, allocation: Allocation) = Attendance(
    scheduledInstance = instance,
    prisonerNumber = allocation.prisonerNumber,
    attendanceReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.AUTO_SUSPENDED),
    initialIssuePayment = false,
    status = AttendanceStatus.COMPLETED,
    recordedTime = LocalDateTime.now(),
    recordedBy = ServiceName.SERVICE_NAME.value,
  )

  private fun cancelledAttendance(instance: ScheduledInstance, allocation: Allocation) = Attendance(
    scheduledInstance = instance,
    prisonerNumber = allocation.prisonerNumber,
    initialIssuePayment = instance.isPaid(),
    status = AttendanceStatus.COMPLETED,
    attendanceReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED),
    comment = instance.cancelledReason,
    recordedTime = instance.cancelledTime,
    recordedBy = instance.cancelledBy,
  )

  private fun waitingAttendance(instance: ScheduledInstance, allocation: Allocation) = Attendance(
    scheduledInstance = instance,
    prisonerNumber = allocation.prisonerNumber,
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
