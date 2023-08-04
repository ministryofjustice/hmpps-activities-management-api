package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList

@Repository
interface WaitingListRepository : JpaRepository<WaitingList, Long> {
  fun findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
    prisonCode: String,
    prisonerNumber: String,
    activitySchedule: ActivitySchedule,
  ): List<WaitingList>

  fun findByActivitySchedule(activitySchedule: ActivitySchedule): List<WaitingList>
}
