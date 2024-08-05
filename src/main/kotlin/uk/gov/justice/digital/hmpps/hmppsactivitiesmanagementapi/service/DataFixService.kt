package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import java.time.LocalDate

@Service
class DataFixService(
  private val activityScheduleService: ActivityScheduleService,
  private val activityService: ActivityService,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun deallocate(scheduleId: Long, prisonerNumbers: List<String>, endDate: LocalDate) {
    val request = PrisonerDeallocationRequest(prisonerNumbers = prisonerNumbers, endDate = endDate, reasonCode = "OTHER")
    activityScheduleService.deallocatePrisoners(scheduleId, request, "activities-management-admin-1")
  }

  fun makeUnpaid(prisonCode: String, activityId: Long) {
    val request = ActivityUpdateRequest(pay = emptyList(), paid = false)
    activityService.updateActivity(prisonCode, activityId, request, "activities-management-admin-1", adminMode = true)
  }

  fun allocate(scheduleId: Long, prisonerNumbers: List<Pair<String, LocalDate>>) {
    prisonerNumbers.forEach { prisoner ->
      val request = PrisonerAllocationRequest(prisonerNumber = prisoner.first, startDate = prisoner.second)
      activityScheduleService.allocatePrisoner(scheduleId, request, "activities-management-admin-1", adminMode = true)
    }
  }
}
