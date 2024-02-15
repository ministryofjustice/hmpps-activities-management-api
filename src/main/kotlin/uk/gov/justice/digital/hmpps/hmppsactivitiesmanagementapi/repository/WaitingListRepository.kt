package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus

@Repository
interface WaitingListRepository : JpaRepository<WaitingList, Long>, JpaSpecificationExecutor<WaitingList> {
  fun findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
    prisonCode: String,
    prisonerNumber: String,
    activitySchedule: ActivitySchedule,
  ): List<WaitingList>

  fun findByActivitySchedule(activitySchedule: ActivitySchedule): List<WaitingList>
  fun findByPrisonCodeAndPrisonerNumberAndStatusIn(prisonCode: String, prisonerNumber: String, statuses: Set<WaitingListStatus>): List<WaitingList>
  fun findByPrisonCodeAndStatusIn(prisonCode: String, statuses: Set<WaitingListStatus>): List<WaitingList>
  fun findByPrisonCodeAndPrisonerNumber(prisonCode: String, prisonerNumber: String): List<WaitingList>
  fun findByActivityAndStatusIn(activity: Activity, statuses: Set<WaitingListStatus>): List<WaitingList>

  @Query(value = "UPDATE WaitingList w SET w.prisonerNumber = :newNumber, w.bookingId = coalesce(:newBookingId, w.bookingId) WHERE w.prisonerNumber = :oldNumber")
  @Modifying
  fun mergeOldPrisonerNumberToNew(oldNumber: String, newNumber: String, newBookingId: Long?)

  @Query(value = "UPDATE WaitingList w SET w.bookingId = coalesce(:newBookingId, w.bookingId) WHERE w.prisonerNumber = :prisonerNumber")
  @Modifying
  fun mergePrisonerToNewBookingId(prisonerNumber: String, newBookingId: Long?)
}
