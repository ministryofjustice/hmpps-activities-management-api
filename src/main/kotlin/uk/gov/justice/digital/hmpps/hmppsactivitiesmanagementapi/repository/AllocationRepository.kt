package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BookingCount
import java.time.LocalDate

interface CandidateAllocation {
  fun getAllocationId(): Long
  fun getPrisonerNumber(): String
  fun getCode(): String
}

@Repository
interface AllocationRepository : JpaRepository<Allocation, Long> {

  fun findByAllocationIdIn(ids: List<Long>): List<Allocation>

  @Query(
    value =
    "FROM Allocation a " +
      "WHERE a.prisonerNumber IN (:prisonerNumbers) " +
      "  AND a.activitySchedule.activity.prisonCode = :prisonCode",
  )
  @EntityGraph(attributePaths = ["activitySchedule.activity.activityPay"], type = EntityGraph.EntityGraphType.LOAD)
  fun findByPrisonCodeAndPrisonerNumbers(prisonCode: String, prisonerNumbers: List<String>): List<Allocation>

  @Query(
    value = """
      SELECT 
       a2.allocation_id,
       a2.prisoner_number,
       ac.code
      FROM activity a
      JOIN activity_category ac ON ac.activity_category_id = a.activity_category_id
      JOIN allocation a2 ON a2.activity_schedule_id = a.activity_id
      WHERE a.prison_code = :prisonCode AND a2.prisoner_status != 'ENDED'
       AND a2.activity_schedule_id != :activityScheduleId
    """,
    nativeQuery = true,
  )
  fun getCandidateAllocations(
    @Param("prisonCode") prisonCode: String,
    @Param("activityScheduleId") activityScheduleId: Long,
  ): List<CandidateAllocation>

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
  fun findByPrisonCodePrisonerNumberPrisonerStatus(
    prisonCode: String,
    prisonerNumber: String,
    vararg prisonerStatus: PrisonerStatus,
  ): List<Allocation>

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

  @Query(
    value = """
      select new uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BookingCount(a.bookingId, count(a)) from Allocation a 
      join ActivitySchedule as2 on a.activitySchedule.activityScheduleId = as2.activityScheduleId 
      join Activity a2 on as2.activity.activityId = a2.activityId 
      where a2.prisonCode = :prisonCode 
      and a.prisonerStatus = :prisonerStatus
      group by a.bookingId
      order by a.bookingId
      """,
  )
  fun findBookingAllocationCountsByPrisonAndPrisonerStatus(
    prisonCode: String,
    prisonerStatus: PrisonerStatus,
  ): List<BookingCount>

  @Query(
    "select case when count(a) > 0 then true else false end " +
      "from Allocation a " +
      "where a.activitySchedule.activity.prisonCode = :prisonCode " +
      "and a.prisonerNumber = :prisonerNumber " +
      "and a.prisonerStatus IN (:prisonerStatus)",
  )
  fun existAtPrisonForPrisoner(
    prisonCode: String,
    prisonerNumber: String,
    prisonerStatus: Collection<PrisonerStatus>,
  ): Boolean

  @Query(
    value =
    "FROM Allocation a " +
      "WHERE a.prisonerStatus = :prisonerStatus " +
      "  AND a.startDate <=  :startDate " +
      "  AND a.activitySchedule.activity.prisonCode = :prisonCode",
  )
  fun findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
    prisonCode: String,
    prisonerStatus: PrisonerStatus,
    startDate: LocalDate,
  ): List<Allocation>

  @Query(value = "UPDATE Allocation a SET a.prisonerNumber = :newNumber, a.bookingId = coalesce(:newBookingId, a.bookingId) WHERE a.prisonerNumber = :oldNumber")
  @Modifying
  fun mergeOldPrisonerNumberToNew(oldNumber: String, newNumber: String, newBookingId: Long?)

  @Query(value = "UPDATE Allocation a SET a.bookingId = coalesce(:newBookingId, a.bookingId) WHERE a.prisonerNumber = :prisonerNumber")
  @Modifying
  fun mergePrisonerToNewBookingId(prisonerNumber: String, newBookingId: Long?)
}
