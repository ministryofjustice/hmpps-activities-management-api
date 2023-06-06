package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderNonAssociationDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Education
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerAlert
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCandidate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AllocationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.PrisonerAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.EducationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.IncentiveLevelSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.NonAssociationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.ReleaseDateSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.WRASuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.time.LocalDate

@Service
class CandidatesService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val activityScheduleService: ActivityScheduleService,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val allocationsService: AllocationsService,
) {

  fun candidateSuitability(scheduleId: Long, prisonerNumber: String): AllocationSuitability {
    val schedule = activityScheduleRepository.findOrThrowNotFound(scheduleId)

    val prisonerNumbers = schedule.allocations().map { it.prisonerNumber }.toMutableList()
    prisonerNumbers.add(prisonerNumber)

    val prisonerDetails = prisonerSearchApiClient.findByPrisonerNumbers(prisonerNumbers).block()!!
      .associateBy { it.prisonerNumber }
    val candidateDetails = prisonerDetails[prisonerNumber]
      ?: throw EntityNotFoundException("Prisoner '$prisonerNumber' not found")

    val prisonerEducation = this.prisonApiClient.getEducationLevels(listOf(prisonerNumber))
    val prisonerNonAssociations = this.prisonApiClient.getOffenderNonAssociations(prisonerNumber)

    return AllocationSuitability(
      workplaceRiskAssessment = wraSuitability(schedule.activity.riskLevel, candidateDetails.alerts),
      incentiveLevel = incentiveLevelSuitability(schedule.activity.activityPay(), candidateDetails.currentIncentive),
      education = educationSuitability(schedule.activity.activityMinimumEducationLevel(), prisonerEducation),
      releaseDate = releaseDateSuitability(schedule.startDate, candidateDetails),
      nonAssociation = nonAssociationSuitability(prisonerDetails.values.toList(), prisonerNonAssociations),
    )
  }

  fun getActivityCandidates(
    scheduleId: Long,
    suitableIncentiveLevels: List<String>?,
    suitableRiskLevels: List<String>?,
    suitableForEmployed: Boolean?,
    searchString: String?,
  ): List<ActivityCandidate> {
    val schedule = activityScheduleService.getScheduleById(scheduleId)
    val prisonCode = schedule.activity.prisonCode

    var prisoners =
      prisonerSearchApiClient.getAllPrisonersInPrison(prisonCode).block()!!
        .content
        .filter { it.status == "ACTIVE IN" && it.legalStatus !== Prisoner.LegalStatus.DEAD }
        .filter { p -> !schedule.allocations.map { it.prisonerNumber }.contains(p.prisonerNumber) }
        .filter { filterByRiskLevel(it, suitableRiskLevels) }
        .filter { filterByIncentiveLevel(it, suitableIncentiveLevels) }
        .filter { filterBySearchString(it, searchString) }

    val prisonerAllocations = allocationsService.findByPrisonCodeAndPrisonerNumbers(
      prisonCode,
      prisoners.map { it.prisonerNumber }.toSet(),
    )

    prisoners =
      prisoners.filter { filterByEmployment(it, prisonerAllocations, suitableForEmployed) }

    val educationLevels =
      this.prisonApiClient.getEducationLevels(prisoners.map { it.prisonerNumber })

    return prisoners.map {
      val thisPersonsAllocations =
        prisonerAllocations.find { a -> it.prisonerNumber == a.prisonerNumber }?.allocations
          ?: emptyList()

      val thisPersonsEducationLevels =
        educationLevels.filter { e -> e.bookingId.toString() == it.bookingId }

      ActivityCandidate(
        name = "${it.firstName} ${it.lastName}",
        prisonerNumber = it.prisonerNumber,
        cellLocation = it.cellLocation,
        otherAllocations = thisPersonsAllocations,
        releaseDate = it.releaseDate,
        educationLevels = thisPersonsEducationLevels,
      )
    }
  }

  private fun filterByRiskLevel(prisoner: Prisoner, suitableRiskLevels: List<String>?): Boolean {
    return suitableRiskLevels == null ||

      (
        suitableRiskLevels.contains("NONE") &&
          (prisoner.alerts ?: emptyList()).none { it.alertType == "R" }
        ) ||

      suitableRiskLevels.contains(
        (prisoner.alerts ?: emptyList())
          .filter { it.alertType == "R" }
          .map { it.alertCode }
          .firstOrNull(),
      )
  }

  private fun filterByIncentiveLevel(
    prisoner: Prisoner,
    suitableIncentiveLevels: List<String>?,
  ): Boolean {
    return suitableIncentiveLevels == null ||
      suitableIncentiveLevels.contains(prisoner.currentIncentive?.level?.description)
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
      ?.filter{ it.active }
      ?.filter { arrayOf("RLO", "RME", "RHI").contains(it.alertCode) }
      ?.map { it.alertCode }

    val prisonerHighestRiskLevel = when {
      prisonerRiskCodes?.contains("RHI") == true -> "high"
      prisonerRiskCodes?.contains("RME") == true -> "medium"
      prisonerRiskCodes?.contains("RLO") == true -> "low"
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
    activityPay: List<ActivityPay>,
    prisonerIncentiveLevel: CurrentIncentive?,
  ) = IncentiveLevelSuitability(
    suitable = activityPay.any{ it.incentiveNomisCode == prisonerIncentiveLevel?.level?.code },
    incentiveLevel = prisonerIncentiveLevel?.level?.description,
  )

  private fun educationSuitability(
    activityEducations: List<ActivityMinimumEducationLevel>,
    prisonerEducations: List<Education>?,
  ): EducationSuitability {
    val suitable = activityEducations.all { activityEducation ->
      prisonerEducations?.any {
        it.educationLevel == activityEducation.educationLevelCode && it.studyArea == activityEducation.studyAreaCode
      } ?: false
    }

    return EducationSuitability(
      suitable = suitable,
      education = prisonerEducations,
    )
  }

  private fun nonAssociationSuitability(
    allocatedPrisoners: List<Prisoner>,
    nonAssociations: List<OffenderNonAssociationDetail>?,
  ): NonAssociationSuitability {
    val allocatedPrisonerNumbers = allocatedPrisoners.map { it.prisonerNumber }

    val allocationNonAssociations = nonAssociations?.filter {
      allocatedPrisonerNumbers.contains(it.offenderNonAssociation.offenderNo)
    }

    return NonAssociationSuitability(
      allocationNonAssociations.isNullOrEmpty(),
      allocationNonAssociations,
    )
  }

  private fun releaseDateSuitability(
    activityStartDate: LocalDate,
    prisonerDetail: Prisoner,
  ) = ReleaseDateSuitability(
    suitable = prisonerDetail.releaseDate?.isAfter(activityStartDate) ?: false,
    earliestReleaseDate = prisonerDetail.releaseDate,
  )
}
