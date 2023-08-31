package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus

@Repository
interface AllocationRepository : JpaRepository<Allocation, Long> {
  @Query(
    value =
    "FROM Allocation a " +
      "WHERE a.prisonerNumber IN (:prisonerNumbers) " +
      "  AND a.activitySchedule.activity.prisonCode = :prisonCode",
  )
  @EntityGraph(attributePaths = ["activitySchedule.activity.activityPay"], type = EntityGraph.EntityGraphType.LOAD)
  fun findByPrisonCodeAndPrisonerNumbers(prisonCode: String, prisonerNumbers: List<String>): List<Allocation>

  @Query(
    value =
    "FROM Allocation a " +
      "WHERE a.prisonerNumber = :prisonerNumber " +
      "  AND a.activitySchedule.activity.prisonCode = :prisonCode",
  )
  fun findByPrisonCodeAndPrisonerNumber(prisonCode: String, prisonerNumber: String): List<Allocation>

  @Query(
    value =
    "FROM Allocation a " +
      "WHERE a.prisonerNumber = :prisonerNumber " +
      "  AND a.prisonerStatus IN (:prisonerStatus) " +
      "  AND a.activitySchedule.activity.prisonCode = :prisonCode",
  )
  fun findByPrisonCodePrisonerNumberPrisonerStatus(prisonCode: String, prisonerNumber: String, vararg prisonerStatus: PrisonerStatus): List<Allocation>

  @Query(
    value =
    "FROM Allocation a " +
      "WHERE a.prisonerStatus = :prisonerStatus " +
      "  AND a.activitySchedule.activity.prisonCode = :prisonCode",
  )
  fun findByPrisonCodePrisonerStatus(prisonCode: String, prisonerStatus: PrisonerStatus): List<Allocation>

  @Query(
    value =
    "FROM Allocation a " +
      "WHERE a.allocationId = :allocationId " +
      "  AND a.activitySchedule.activity.prisonCode = :prisonCode",
  )
  fun findByAllocationIdAndPrisonCode(allocationId: Long, prisonCode: String): Allocation?
}
