package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DataFix
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.DataFixRepository
import java.time.LocalDate

@Service
class DataFixService(
  private val activityScheduleService: ActivityScheduleService,
  private val activityService: ActivityService,
  private val dataFixRepository: DataFixRepository,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun deallocate(activityScheduleId: Long) {
    activityScheduleService.getScheduleById(activityScheduleId, LocalDate.now(), adminMode = true).let {
      val allocations = it.allocations.filterNot { it.status == PrisonerStatus.ENDED }
      val allocatedPrisonersList = allocations.map { it.prisonerNumber }

      val request = PrisonerDeallocationRequest(prisonerNumbers = allocatedPrisonersList, endDate = LocalDate.now(), reasonCode = "OTHER")
      activityScheduleService.deallocatePrisoners(activityScheduleId, request, "activities-management-admin-1")
      this.saveAllocations(activityScheduleId, allocations)
    }
  }

  fun makeUnpaid(prisonCode: String, activityScheduleId: Long) {
    val activityId = activityScheduleService.getScheduleById(activityScheduleId, LocalDate.now(), adminMode = true).activity.id
    val request = ActivityUpdateRequest(pay = emptyList(), paid = false)
    activityService.updateActivity(prisonCode, activityId, request, "activities-management-admin-1", adminMode = true)
  }

  @Transactional
  fun reallocate(activityScheduleId: Long) {
    val dataFixes = dataFixRepository.findByActivityScheduleId(activityScheduleId)
    val activitySchedule = activityScheduleService.getScheduleById(activityScheduleId, LocalDate.now(), adminMode = true)

    val nextScheduledInstance = activitySchedule.instances.filter { it.date >= LocalDate.now() }.sortedWith(compareBy({ it.date }, { it.startTime })).first()

    dataFixes.forEach { dataFix ->
      dataFix.takeIf { it.prisonerStatus == PrisonerStatus.ACTIVE || it.prisonerStatus == PrisonerStatus.PENDING }?.let {
        val reallocateNextSession = it.startDate <= LocalDate.now()
        val startDate = if (reallocateNextSession) LocalDate.now() else dataFix.startDate
        val scheduledInstanceId = if (reallocateNextSession) nextScheduledInstance.id else null
        val request = PrisonerAllocationRequest(prisonerNumber = dataFix.prisonerNumber, startDate = startDate, scheduleInstanceId = scheduledInstanceId)
        activityScheduleService.allocatePrisoner(activityScheduleId, request, "activities-management-admin-1", adminMode = true)
      }
    }
    dataFixRepository.deleteAll(dataFixes)
  }

  private fun saveAllocations(activityScheduleId: Long, allocations: List<Allocation>) {
    allocations.forEach {
      val dataFix = DataFix(activityScheduleId = activityScheduleId, prisonerNumber = it.prisonerNumber, startDate = it.startDate, prisonerStatus = it.prisonerStatus?.let { it1 -> PrisonerStatus.valueOf(it1) })
      dataFixRepository.save(dataFix)
    }
  }
}
