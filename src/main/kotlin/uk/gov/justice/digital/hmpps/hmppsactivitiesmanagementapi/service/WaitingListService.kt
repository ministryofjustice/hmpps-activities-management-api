package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isActiveInPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isActiveOutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess

@Service
@Transactional(readOnly = true)
class WaitingListService(
  private val scheduleRepository: ActivityScheduleRepository,
  private val prisonApiClient: PrisonApiClient,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  fun addPrisoner(prisonCode: String, application: WaitingListApplicationRequest, createdBy: String) {
    checkCaseloadAccess(prisonCode)

    val schedule = scheduleRepository.findBy(application.activityScheduleId!!, prisonCode)
      ?: throw EntityNotFoundException("Activity schedule ${application.activityScheduleId} not found")

    schedule.addToWaitingList(
      prisonerNumber = application.prisonerNumber!!,
      bookingId = getActivePrisonerBookingId(prisonCode, application),
      applicationDate = application.applicationDate!!,
      requestedBy = application.requestedBy!!,
      comments = application.comments,
      createdBy = createdBy,
      status = application.status!!.waitingListStatus,
    )

    scheduleRepository.saveAndFlush(schedule)

    log.info("Added prisoner ${application.prisonerNumber} to the waiting list for activity schedule ${application.activityScheduleId}")
  }

  private fun getActivePrisonerBookingId(prisonCode: String, request: WaitingListApplicationRequest): Long {
    val prisonerDetails = prisonApiClient.getPrisonerDetails(request.prisonerNumber!!).block()
      ?: throw IllegalArgumentException("Prisoner with ${request.prisonerNumber} not found")

    require(prisonerDetails.isActiveInPrison(prisonCode) || prisonerDetails.isActiveOutPrison(prisonCode)) {
      "Prisoner ${request.prisonerNumber} is not active in/out at prison $prisonCode"
    }

    requireNotNull(prisonerDetails.bookingId) {
      "Prisoner ${request.prisonerNumber} has no booking id at prison $prisonCode"
    }

    return prisonerDetails.bookingId
  }
}
