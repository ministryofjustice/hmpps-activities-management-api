package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import java.time.LocalDate

interface ScheduledInstanceRepository : JpaRepository<ScheduledInstance, Long> {
  @Query(
    "SELECT si FROM ScheduledInstance si WHERE EXISTS(" +
      "SELECT 1 FROM si.activitySchedule.allocations a " +
      "WHERE a.activitySchedule.activity.prisonCode = :prisonCode " +
      "AND a.prisonerNumber = :prisonerNumber) " +
      "AND si.sessionDate >= :startDate " +
      "AND si.sessionDate <= :endDate "
  )
  fun getActivityScheduleInstancesByPrisonerNumberAndDateRange(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate
  ): List<ScheduledInstance>
}
