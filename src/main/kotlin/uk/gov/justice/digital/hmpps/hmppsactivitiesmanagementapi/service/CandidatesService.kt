package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api.NonAssociationsApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api.extensions.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Education
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isActiveAtPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerAlert
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.ActivityCategoryCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCandidate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AllocationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.AllocationPayRate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.DeallocationCaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.EducationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.IncentiveLevelSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.ReleaseDateSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.WRASuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.CandidateAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.determineEarliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.hasNonAssociations
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class CandidatesService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val nonAssociationsApiClient: NonAssociationsApiClient,
  private val caseNotesApiClient: CaseNotesApiClient,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val allocationRepository: AllocationRepository,
  private val waitingListRepository: WaitingListRepository,
) {
  fun candidateSuitability(scheduleId: Long, prisonerNumber: String): AllocationSuitability {
    val schedule = activityScheduleRepository.findOrThrowNotFound(scheduleId)

    val candidateDetails = prisonerSearchApiClient.findByPrisonerNumber(prisonerNumber)
      ?: throw IllegalArgumentException("Prisoner number '$prisonerNumber' not found")

    val prisonerEducation = prisonApiClient.getEducationLevels(listOf(prisonerNumber))

    val candidateAllocations = allocationRepository.findByPrisonCodeAndPrisonerNumber(
      candidateDetails.prisonId!!,
      candidateDetails.prisonerNumber,
    )

    val currentAllocations = candidateAllocations
      .filterNot { allocation -> allocation.isEnded() }
      .map { allocation ->
        AllocationPayRate(
          allocation = allocation.toModel(),
          payRate = candidateDetails.currentIncentive?.level?.code?.let {
            allocation.allocationPay(it)
          }?.toModelLite(),
        )
      }

    val previousDeallocations = candidateAllocations
      .filter { allocation -> allocation.activitySchedule.activityScheduleId == scheduleId }
      .filter { allocation -> DeallocationReason.displayedDeallocationReasons().contains(allocation.deallocatedReason) }
      .map { allocation ->
        DeallocationCaseNote(
          allocation = allocation.toModel(),
          caseNoteText = if (allocation.deallocationCaseNoteId != null) caseNotesApiClient.getCaseNote(allocation.prisonerNumber, allocation.deallocationCaseNoteId!!).text else null,
        )
      }

    return AllocationSuitability(
      workplaceRiskAssessment = wraSuitability(schedule.activity.riskLevel, candidateDetails.alerts),
      incentiveLevel = incentiveLevelSuitability(schedule.activity, candidateDetails.currentIncentive),
      education = educationSuitability(schedule.activity.activityMinimumEducationLevel(), prisonerEducation),
      releaseDate = releaseDateSuitability(schedule.startDate, candidateDetails),
      allocations = currentAllocations,
      previousDeallocations = previousDeallocations,
    )
  }

  fun getActivityCandidates(
    scheduleId: Long,
    suitableIncentiveLevels: List<String>?,
    suitableRiskLevels: List<String>?,
    suitableForEmployed: Boolean?,
    noAllocations: Boolean?,
    search: String?,
    pageable: Pageable,
  ): Page<ActivityCandidate> = runBlocking {
    val schedule = activityScheduleRepository.findOrThrowNotFound(scheduleId)
    val prisonCode = schedule.activity.prisonCode
    checkCaseloadAccess(prisonCode)

    val waitingList = waitingListRepository.findByActivitySchedule(schedule)
      .filter { it.isStatus(WaitingListStatus.APPROVED, WaitingListStatus.PENDING) }

    val prisonerAllocations =
      allocationRepository.getCandidateAllocations(prisonCode = prisonCode)
        .groupBy { it.getPrisonerNumber() }

    val prisoners = getPrisonerCandidates(
      prisonCode = prisonCode,
      activityScheduleId = scheduleId,
      waitingList = waitingList,
      prisonerAllocations = prisonerAllocations,
      suitableForEmployed = suitableForEmployed,
      suitableRiskLevels = suitableRiskLevels,
      suitableIncentiveLevels = suitableIncentiveLevels,
      noAllocations = noAllocations,
      search = search,
    )

    val prisonerCount = prisoners.count()
    val start = pageable.offset.toInt()
    val end = (start + pageable.pageSize).coerceAtMost(prisonerCount)

    val candidates = prisoners
      .sortedBy { it.lastName }
      .filterIndexed { index, _ -> index >= start.coerceAtMost(end) && index < end }
      .toList()

    val candidatePrisonerNumbers = candidates.map { it.prisonerNumber }

    val nonAssociations = async { nonAssociationsApiClient.getNonAssociationsInvolving(prisonCode, candidatePrisonerNumbers) }

    PageImpl(
      candidates.map { prisoner ->
        val thisPersonsAllocations = prisonerAllocations[prisoner.prisonerNumber]?.map { it.getAllocationId() }?.let { ids ->
          allocationRepository.findByAllocationIdIn(ids).map { it.toModel() }
        }

        ActivityCandidate(
          name = "${prisoner.firstName} ${prisoner.lastName}",
          prisonerNumber = prisoner.prisonerNumber,
          cellLocation = prisoner.cellLocation,
          otherAllocations = thisPersonsAllocations ?: emptyList(),
          earliestReleaseDate = determineEarliestReleaseDate(prisoner),
          nonAssociations = nonAssociations.await()?.hasNonAssociations(prisoner.prisonerNumber),
        )
      },
      pageable,
      prisonerCount.toLong(),
    )
  }

  fun nonAssociations(
    scheduleId: Long,
    prisonerNumber: String,
  ): List<NonAssociationDetails> {
    val schedule = activityScheduleRepository.findOrThrowNotFound(scheduleId)

    val allocatedPrisoners = schedule.allocations(true).map { it.prisonerNumber }

    return nonAssociationsApiClient.getOffenderNonAssociations(prisonerNumber)
      .map { it.toModel(allocatedPrisoners.contains(it.otherPrisonerDetails.prisonerNumber)) }
  }

  private fun getPrisonerCandidates(
    prisonCode: String,
    activityScheduleId: Long,
    waitingList: List<WaitingList>,
    prisonerAllocations: Map<String, List<CandidateAllocation>>,
    suitableIncentiveLevels: List<String>?,
    suitableRiskLevels: List<String>?,
    suitableForEmployed: Boolean?,
    noAllocations: Boolean?,
    search: String?,
  ): Sequence<Prisoner> =
    prisonerSearchApiClient.getAllPrisonersInPrison(prisonCode).block()!!.content
      .asSequence()
      .filter {
        val prisonerAllocation = prisonerAllocations[it.prisonerNumber] ?: emptyList()
        it.isActiveAtPrison(prisonCode) &&
          it.legalStatus != Prisoner.LegalStatus.DEAD &&
          it.currentIncentive != null &&
          filterByRiskLevel(it, suitableRiskLevels) &&
          filterByIncentiveLevel(it, suitableIncentiveLevels) &&
          filterBySearchString(it, search) &&
          !prisonerAllocation.any { p -> p.getActivityScheduleId() == activityScheduleId } &&
          (noAllocations != true || prisonerAllocation.isEmpty()) &&
          !waitingList.any { w -> w.prisonerNumber == it.prisonerNumber } &&
          filterByEmployment(
            prisonerAllocations = prisonerAllocation,
            suitableForEmployed = suitableForEmployed,
          )
      }

  private fun filterByRiskLevel(prisoner: Prisoner, suitableRiskLevels: List<String>?): Boolean {
    val riskAssessmentCodes = listOf("RLO", "RME", "RHI")

    return suitableRiskLevels == null ||

      suitableRiskLevels.contains("NONE") &&
      (prisoner.alerts ?: emptyList())
        .none { it.alertType == "R" && riskAssessmentCodes.contains(it.alertCode) } ||

      suitableRiskLevels.contains(
        (prisoner.alerts ?: emptyList())
          .filter { it.alertType == "R" }
          .map { it.alertCode }
          .firstOrNull { riskAssessmentCodes.contains(it) },
      )
  }

  private fun filterByIncentiveLevel(
    prisoner: Prisoner,
    suitableIncentiveLevels: List<String>?,
  ): Boolean {
    return suitableIncentiveLevels == null ||
      suitableIncentiveLevels.contains(prisoner.currentIncentive!!.level.description)
  }

  private fun filterByEmployment(
    prisonerAllocations: List<CandidateAllocation>,
    suitableForEmployed: Boolean?,
  ): Boolean {
    suitableForEmployed ?: return true
    val employmentAllocations = prisonerAllocations.filter { it.getCode() != ActivityCategoryCode.SAA_NOT_IN_WORK.name }

    return employmentAllocations.isNotEmpty() == suitableForEmployed
  }

  private fun filterBySearchString(
    prisoner: Prisoner,
    searchString: String?,
  ): Boolean {
    val searchStringLower = searchString?.lowercase()

    return searchStringLower == null ||
      prisoner.prisonerNumber.lowercase().contains(searchStringLower) ||
      "${prisoner.firstName} ${prisoner.lastName}".lowercase().contains(searchStringLower)
  }

  private fun wraSuitability(
    activityRiskLevel: String,
    prisonerAlerts: List<PrisonerAlert>?,
  ): WRASuitability {
    val prisonerRiskCodes = prisonerAlerts
      ?.filter { it.active }
      ?.filter { WORKPLACE_RISK_LEVELS.contains(it.alertCode) }
      ?.map { it.alertCode } ?: emptyList()

    val prisonerHighestRiskLevel = when {
      prisonerRiskCodes.contains(WORKPLACE_RISK_LEVEL_HIGH) -> "high"
      prisonerRiskCodes.contains(WORKPLACE_RISK_LEVEL_MEDIUM) -> "medium"
      prisonerRiskCodes.contains(WORKPLACE_RISK_LEVEL_LOW) -> "low"
      else -> "none"
    }

    val suitable = when (activityRiskLevel) {
      "low" -> "low" == prisonerHighestRiskLevel
      "medium" -> listOf("low", "medium").contains(prisonerHighestRiskLevel)
      else -> true
    }

    return WRASuitability(
      suitable,
      prisonerHighestRiskLevel,
    )
  }

  private fun incentiveLevelSuitability(
    activity: Activity,
    prisonerIncentiveLevel: CurrentIncentive?,
  ) = IncentiveLevelSuitability(
    suitable = !activity.isPaid() || activity.activityPay().any { it.incentiveNomisCode == prisonerIncentiveLevel?.level?.code },
    incentiveLevel = prisonerIncentiveLevel?.level?.description,
  )

  private fun educationSuitability(
    activityEducations: List<ActivityMinimumEducationLevel>,
    prisonerEducations: List<Education>,
  ): EducationSuitability {
    val suitable = activityEducations.all { activityEducation ->
      // TODO:
      // We should be comparing against education codes here instead of matching against descriptions which could be
      // outdated / unreliable, however currently the prison API endpoint doesn't return education codes to do this.
      // The API endpoint (GET /api/education/prisoner/{offenderNo}) will need be updated to include this, then this
      // method should be updated
      prisonerEducations.any {
        it.educationLevel == activityEducation.educationLevelDescription &&
          it.studyArea == activityEducation.studyAreaDescription
      }
    }

    return EducationSuitability(
      suitable = suitable,
      education = prisonerEducations,
    )
  }

  private fun releaseDateSuitability(
    activityStartDate: LocalDate,
    prisonerDetail: Prisoner,
  ): ReleaseDateSuitability {
    val earliestReleaseDate = determineEarliestReleaseDate(prisonerDetail)
    return ReleaseDateSuitability(
      suitable = earliestReleaseDate.releaseDate?.isAfter(activityStartDate) ?: true,
      earliestReleaseDate = earliestReleaseDate,
    )
  }

  companion object {
    const val WORKPLACE_RISK_LEVEL_LOW = "RLO"
    const val WORKPLACE_RISK_LEVEL_MEDIUM = "RME"
    const val WORKPLACE_RISK_LEVEL_HIGH = "RHI"
    val WORKPLACE_RISK_LEVELS = listOf(
      WORKPLACE_RISK_LEVEL_LOW,
      WORKPLACE_RISK_LEVEL_MEDIUM,
      WORKPLACE_RISK_LEVEL_HIGH,
    )
  }
}
