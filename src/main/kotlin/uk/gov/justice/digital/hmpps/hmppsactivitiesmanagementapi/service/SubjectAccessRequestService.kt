package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SubjectAccessRequestData
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.SarRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
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
class SubjectAccessRequestService(private val repository: SarRepository, private val referenceCodeService: ReferenceCodeService) : HmppsPrisonSubjectAccessRequestService {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun getPrisonContentFor(prn: String, fromDate: LocalDate?, toDate: LocalDate?): HmppsSubjectAccessRequestContent? {
    log.info("SAR: processing subject access request for prisoner $prn")

    val from = fromDate ?: LocalDate.EPOCH
    val to = toDate ?: LocalDate.now()

    val allocations = repository.findAllocationsBy(prn, from, to)
    val waitingLists = repository.findWaitingListsBy(prn, from, to)
    val appointments = repository.findAppointmentsBy(prn, from, to)
    val allAttendance = repository.findAttendanceBy(prn, from, to)

    val referenceCodesForAppointmentsMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    appointments.forEach { appointment ->
      appointment.category = referenceCodesForAppointmentsMap[appointment.categoryCode]?.description
        ?: "Unknown category"
    }

    return if (allocations.isEmpty() && waitingLists.isEmpty() && appointments.isEmpty() && allAttendance.isEmpty()) {
      log.info("SAR: no data found for subject access request for prisoner $prn for dates $from to date $to")
      null
    } else {
      log.info("SAR: data found for subject access request for prisoner $prn for dates $from to date $to")

      val attendanceSummary: List<SarAttendanceSummary> =
        allAttendance.groupingBy { it.attendanceReasonDescription }.eachCount().mapNotNull { it.key?.let { it1 -> ModelSarAttendanceSummary(it1, it.value) } }

      HmppsSubjectAccessRequestContent(
        SubjectAccessRequestData(
          prisonerNumber = prn,
          fromDate = from,
          toDate = to,
          allocations = allocations.map(::ModelSarAllocation),
          attendanceSummary = attendanceSummary,
          waitingListApplications = waitingLists.map(::ModelSarWaitingList),
          appointments = appointments.map(::ModelSarAppointment),
        ),
      )
    }
  }
}
