package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SubjectAccessRequestContent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.SarRepository
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarAllocation as ModelSarAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarAppointment as ModelSarAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarAttendanceSummary as ModelSarAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarWaitingList as ModelSarWaitingList

/**
 * Prisoners have the right to access and receive a copy of their personal data and other supplementary information.
 *
 * This is commonly referred to as a subject access request or ‘SAR’.
 *
 * The purpose of this service is to surface all relevant prisoner specific information for a subject access request.
 */
@Service
class SubjectAccessRequestService(private val repository: SarRepository) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getContentFor(prisonerNumber: String, fromDate: LocalDate?, toDate: LocalDate?): SubjectAccessRequestContent? {
    log.info("SAR: processing subject access request for prisoner $prisonerNumber")

    val from = fromDate ?: LocalDate.now()
    val to = toDate ?: LocalDate.now()

    val allocations = repository.findAllocationsBy(prisonerNumber, from, to)
    val waitingLists = repository.findWaitingListsBy(prisonerNumber, from, to)
    val appointments = repository.findAppointmentsBy(prisonerNumber, from, to)
    val allAttendance = repository.findAttendanceBy(prisonerNumber, from, to)

    return if (allocations.isEmpty() && waitingLists.isEmpty() && appointments.isEmpty() && allAttendance.isEmpty()) {
      log.info("SAR: no data found for subject access request for prisoner $prisonerNumber for dates $from to date $to")
      null
    } else {
      log.info("SAR: data found for subject access request for prisoner $prisonerNumber for dates $from to date $to")

      val attendanceSummary: List<SarAttendanceSummary> =
        allAttendance.groupingBy { it.attendanceReasonCode }.eachCount().mapNotNull { it.key?.let { it1 -> ModelSarAttendanceSummary(it1, it.value) } }

      SubjectAccessRequestContent(
        prisonerNumber = prisonerNumber,
        fromDate = from,
        toDate = to,
        allocations = allocations.map(::ModelSarAllocation),
        attendanceSummary = attendanceSummary,
        waitingListApplications = waitingLists.map(::ModelSarWaitingList),
        appointments = appointments.map(::ModelSarAppointment),
      )
    }
  }
}
