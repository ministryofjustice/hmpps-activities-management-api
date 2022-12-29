package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.UniquePropertyId
import java.time.LocalDate

/**
 * This repository is READ-ONLY and uses the view V_PRISONER_SCHEDULED_ACTIVITIES.
 * This provides a convenient, single entity source for prisoner schedules (multiple joins)
 */

interface PrisonerScheduledActivityRepository : JpaRepository<PrisonerScheduledActivity, UniquePropertyId> {

  @Query(
    """
    SELECT sa FROM PrisonerScheduledActivity sa 
    WHERE sa.prisonCode = :prisonCode
    AND sa.sessionDate >= :startDate
    AND sa.sessionDate <= :endDate
    AND sa.prisonerNumber = :prisonerNumber
    AND sa.suspended = false
    AND sa.cancelled = false
    """
  )
  fun getScheduledActivitiesForPrisonerAndDateRange(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<PrisonerScheduledActivity>

  @Query(
    """
    SELECT sa FROM PrisonerScheduledActivity sa 
    WHERE sa.prisonCode = :prisonCode
    AND sa.sessionDate = :date
    AND sa.prisonerNumber in :prisonerNumbers
    AND sa.suspended = false
    AND sa.cancelled = false
    """
  )
  fun getScheduledActivitiesForPrisonerListAndDate(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
  ): List<PrisonerScheduledActivity>
}
