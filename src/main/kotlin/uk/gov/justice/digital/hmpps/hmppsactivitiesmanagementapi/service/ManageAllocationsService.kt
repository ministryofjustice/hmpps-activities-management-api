package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.MovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isOut
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.lastMovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonRegimeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ManageAllocationsService(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val activityRepository: ActivityRepository,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val allocationRepository: AllocationRepository,
  private val prisonRegimeRepository: PrisonRegimeRepository,
  private val prisonerSearch: PrisonerSearchApiApplicationClient,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun allocations(operation: AllocationOperation) {
    LocalDateTime.now().let { now ->
      when (operation) {
        AllocationOperation.DEALLOCATE_ENDING -> {
          log.info("Ending allocations due to end today.")
          allocationsDueToEnd().allocations(now, DeallocationReason.ENDED)
        }

        AllocationOperation.DEALLOCATE_EXPIRING -> {
          log.info("Expiring allocations due to expire today.")
          allocationsDueToExpire().allocations(now, DeallocationReason.EXPIRED)
        }
      }
    }
  }

  private fun allocationsDueToEnd(): Map<ActivitySchedule, List<Allocation>> =
    LocalDate.now().let { today ->
      forEachRolledOutPrison()
        .flatMap { prison ->
          activityRepository.getAllForPrisonAndDate(prison.code, today).flatMap { activity ->
            if (activity.ends(today)) {
              activity.schedules().flatMap { it.allocations() }
            } else {
              activity.schedules().flatMap { it.allocations().ending(today) }
            }
          }
        }.groupBy { it.activitySchedule }
    }

  private fun forEachRolledOutPrison() =
    rolloutPrisonRepository.findAll().filter { it.isActivitiesRolledOut() }

  private fun List<Allocation>.ending(date: LocalDate) =
    filter { it.ends(date) && !it.status(PrisonerStatus.ENDED) }

  private fun allocationsDueToExpire(): Map<ActivitySchedule, List<Allocation>> =
    forEachRolledOutPrison()
      .flatMap { prison ->
        val regime = prisonRegimeRepository.findByPrisonCode(prison.code)

        if (regime != null) {
          val candidateExpiredAllocations =
            allocationRepository.findByPrisonCodePrisonerStatus(prison.code, PrisonerStatus.AUTO_SUSPENDED)
              .filter(regime::hasExpired)

          val expiredPrisoners = candidateExpiredAllocations.ifNotEmpty {
            prisonerSearch.findByPrisonerNumbers(candidateExpiredAllocations.map { it.prisonerNumber }.distinct())
              .block()
              ?.filter { it.hasExpired(regime) }
              ?.map { it.prisonerNumber }
              ?.toSet() ?: emptySet()
          }

          candidateExpiredAllocations.filter { expiredPrisoners.contains(it.prisonerNumber) }
        } else {
          log.warn("Rolled out prison ${prison.code} is missing a prison regime.")
          emptyList()
        }
      }.groupBy { it.activitySchedule }

  private fun <T, R> List<T>.ifNotEmpty(block: (List<T>) -> Set<R>) =
    if (this.isNotEmpty()) {
      block(this)
    } else {
      emptySet()
    }

  private fun Prisoner.hasExpired(prisonRegime: PrisonRegime) =
    if (isOut()) {
      when (lastMovementType()) {
        MovementType.RELEASE -> prisonRegime.hasExpired { this.releaseDate }
        MovementType.TEMPORARY_ABSENCE -> true
        MovementType.TRANSFER -> true
        else -> false
      }
    } else {
      false
    }

  private fun Map<ActivitySchedule, List<Allocation>>.allocations(dateTime: LocalDateTime, reason: DeallocationReason) {
    this.keys.forEach { schedule ->
      continueToRunOnFailure(
        block = {
          getOrDefault(schedule, emptyList()).map { allocation -> allocation.deallocate(dateTime, reason) }
            .let {
              if (it.isNotEmpty()) {
                activityScheduleRepository.saveAndFlush(schedule)
                log.info("Deallocated ${it.size} allocation(s) from schedule ${schedule.activityScheduleId} with reason '$reason'.")
              }
            }
        },
        failure = "An error occurred deallocating allocations on activity schedule ${schedule.activityScheduleId}.",
      )
    }
  }

  private fun continueToRunOnFailure(block: () -> Unit, failure: String = "") {
    runCatching {
      block()
    }
      .onFailure { log.error(failure, it) }
  }
}

enum class AllocationOperation {
  DEALLOCATE_ENDING,
  DEALLOCATE_EXPIRING,
}
