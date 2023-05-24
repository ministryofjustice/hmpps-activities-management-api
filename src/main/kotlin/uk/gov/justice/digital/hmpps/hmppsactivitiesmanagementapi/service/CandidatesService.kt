package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCandidate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.PrisonerAllocations

@Service
class CandidatesService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val activityScheduleService: ActivityScheduleService,
  private val allocationsService: AllocationsService,
) {

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
}
