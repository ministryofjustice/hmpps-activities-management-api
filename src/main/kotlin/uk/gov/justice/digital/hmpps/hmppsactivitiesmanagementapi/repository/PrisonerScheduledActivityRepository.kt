package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.UniquePropertyId
import java.time.LocalDate
import java.util.Optional

/**
 * This repository is READ-ONLY and uses the view V_PRISONER_SCHEDULED_ACTIVITIES.
 * This provides a convenient, single entity source for prisoner schedules (multiple joins)
 */

interface PrisonerScheduledActivityRepository : JpaRepository<PrisonerScheduledActivity, UniquePropertyId> {

  fun getAllByScheduledInstanceId(id: Long): List<PrisonerScheduledActivity>

  fun getByScheduledInstanceIdAndPrisonerNumber(id: Long, prisonerNUmber: String): Optional<PrisonerScheduledActivity>

  @Query(
    """
    SELECT sa FROM PrisonerScheduledActivity sa 
    WHERE sa.prisonCode = :prisonCode
    AND sa.sessionDate >= :startDate
    AND sa.sessionDate <= :endDate
    AND sa.prisonerNumber = :prisonerNumber
    AND (:timeSlot IS NULL OR sa.timeSlot = :timeSlot)
    """,
  )
  fun getScheduledActivitiesForPrisonerAndDateRange(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate,
    timeSlot: TimeSlot?,
  ): List<PrisonerScheduledActivity>

  @Query(
    """
    SELECT sa FROM PrisonerScheduledActivity sa 
    WHERE sa.prisonCode = :prisonCode
    AND sa.sessionDate = :date
    AND sa.prisonerNumber in :prisonerNumbers
    AND (:timeSlot IS NULL OR sa.timeSlot = :timeSlot)
    """,
  )
  fun getScheduledActivitiesForPrisonerListAndDate(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): List<PrisonerScheduledActivity>

  @Query(
    """
    SELECT sa FROM PrisonerScheduledActivity sa 
    WHERE (sa.prisonCode = :prisonCode
    AND sa.sessionDate = :date)
    AND (:timeSlot IS NULL OR sa.timeSlot = :timeSlot)
    AND sa.onWing = false
    AND sa.internalLocationId IS NOT NULL
    """,
  )
  fun findByPrisonCodeAndDateAndTimeSlot(
    prisonCode: String,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): List<PrisonerScheduledActivity>

  @Query(
    """
    SELECT sa FROM PrisonerScheduledActivity sa 
    WHERE (sa.prisonCode = :prisonCode
    AND sa.sessionDate = :date
    AND sa.onWing = false
    AND sa.internalLocationId in :internalLocationIds)
    AND (:timeSlot IS NULL OR sa.timeSlot = :timeSlot)
    """,
  )
  fun findByPrisonCodeAndInternalLocationIdsAndDateAndTimeSlot(
    prisonCode: String,
    internalLocationIds: Set<Int>,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): List<PrisonerScheduledActivity>
}

/**
 * Exclude any that are for today but have no attendance as these should not be on such things like unlock lists, etc.
 */
fun List<PrisonerScheduledActivity>.excludeTodayWithoutAttendance() = this.filter { activity -> activity.sessionDate != LocalDate.now() || activity.attendanceStatus != null }
