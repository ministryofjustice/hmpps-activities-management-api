package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api.NonAssociationsApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api.extensions.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Education
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isActiveIn
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isActiveOut
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerAlert
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCandidate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AllocationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.PrisonerAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.AllocationPayRate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.EducationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.IncentiveLevelSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.NonAssociationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.ReleaseDateSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.WRASuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.determineEarliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonerAllocations
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class CandidatesService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val nonAssociationsApiClient: NonAssociationsApiClient,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val allocationRepository: AllocationRepository,
  private val waitingListRepository: WaitingListRepository,
) {
  fun candidateSuitability(scheduleId: Long, prisonerNumber: String): AllocationSuitability {
    val schedule = activityScheduleRepository.findOrThrowNotFound(scheduleId)

    val candidateDetails = prisonerSearchApiClient.findByPrisonerNumber(prisonerNumber)
      ?: throw IllegalArgumentException("Prisoner number '$prisonerNumber' not found")

    val prisonerEducation = prisonApiClient.getEducationLevels(listOf(prisonerNumber))
    val prisonerNonAssociations = nonAssociationsApiClient.getOffenderNonAssociations(prisonerNumber)
    val prisonerNumbers = schedule.allocations(true).map { it.prisonerNumber }

    val candidateAllocations = allocationRepository.findByPrisonCodeAndPrisonerNumber(
      candidateDetails.prisonId!!,
      candidateDetails.prisonerNumber,
    ).map { allocation ->
      AllocationPayRate(
        allocation = allocation.toModel(),
        payRate = candidateDetails.currentIncentive?.level?.code?.let {
          allocation.allocationPay(it)
        }?.toModelLite(),
      )
    }

    return AllocationSuitability(
      workplaceRiskAssessment = wraSuitability(schedule.activity.riskLevel, candidateDetails.alerts),
      incentiveLevel = incentiveLevelSuitability(schedule.activity, candidateDetails.currentIncentive),
      education = educationSuitability(schedule.activity.activityMinimumEducationLevel(), prisonerEducation),
      releaseDate = releaseDateSuitability(schedule.startDate, candidateDetails),
      nonAssociation = nonAssociationSuitability(prisonerNumbers, prisonerNonAssociations),
      allocations = candidateAllocations,
    )
  }

  fun getActivityCandidates(
    scheduleId: Long,
    suitableIncentiveLevels: List<String>?,
    suitableRiskLevels: List<String>?,
    suitableForEmployed: Boolean?,
    searchString: String?,
  ): List<ActivityCandidate> {
    val schedule = activityScheduleRepository.findOrThrowNotFound(scheduleId)
    checkCaseloadAccess(schedule.activity.prisonCode)

    val prisonCode = schedule.activity.prisonCode

    val waitingList = waitingListRepository.findByActivitySchedule(schedule)
      .filter { it.isStatus(WaitingListStatus.APPROVED, WaitingListStatus.PENDING) }

    var prisoners =
      prisonerSearchApiClient.getAllPrisonersInPrison(prisonCode).block()!!
        .content
        .filter { (it.isActiveIn() || it.isActiveOut()) && it.legalStatus != Prisoner.LegalStatus.DEAD && it.currentIncentive != null }
        .filter { p -> !schedule.allocations(true).map { it.prisonerNumber }.contains(p.prisonerNumber) }
        .filter { filterByRiskLevel(it, suitableRiskLevels) }
        .filter { filterByIncentiveLevel(it, suitableIncentiveLevels) }
        .filter { filterBySearchString(it, searchString) }
        .filter { waitingList.none { w -> w.prisonerNumber == it.prisonerNumber } }

    val prisonerAllocations = allocationRepository.findByPrisonCodeAndPrisonerNumbers(
      prisonCode,
      prisoners.map { it.prisonerNumber },
    )
      .filterNot { it.status(PrisonerStatus.ENDED) }
      .toModelPrisonerAllocations()

    prisoners =
      prisoners.filter { filterByEmployment(it, prisonerAllocations, suitableForEmployed) }

    return prisoners.map {
      val thisPersonsAllocations =
        prisonerAllocations.find { a -> it.prisonerNumber == a.prisonerNumber }?.allocations
          ?: emptyList()

      ActivityCandidate(
        name = "${it.firstName} ${it.lastName}",
        prisonerNumber = it.prisonerNumber,
        cellLocation = it.cellLocation,
        otherAllocations = thisPersonsAllocations,
        earliestReleaseDate = determineEarliestReleaseDate(it),
      )
    }
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
    prisoner: Prisoner,
    prisonerAllocations: List<PrisonerAllocations>,
    suitableForEmployed: Boolean?,
  ): Boolean {
    val allocations =
      prisonerAllocations.find { it.prisonerNumber == prisoner.prisonerNumber }?.allocations
        ?: emptyList()

    return suitableForEmployed == null || allocations.isNotEmpty() == suitableForEmployed
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

  private fun nonAssociationSuitability(
    allocatedPrisoners: List<String>,
    nonAssociations: List<PrisonerNonAssociation>,
  ): NonAssociationSuitability {
    val allocationNonAssociations = nonAssociations.filter { allocatedPrisoners.contains(it.otherPrisonerDetails.prisonerNumber) }

    return NonAssociationSuitability(
      allocationNonAssociations.isEmpty(),
      allocationNonAssociations.map { it.toModel() },
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
