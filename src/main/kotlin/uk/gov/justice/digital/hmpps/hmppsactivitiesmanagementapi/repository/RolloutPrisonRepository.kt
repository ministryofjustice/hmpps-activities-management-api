package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison

@Repository
interface RolloutPrisonRepository : JpaRepository<RolloutPrison, Long> {
  fun findByCode(code: String): RolloutPrison?
}

fun RolloutPrisonRepository.isActivitiesRolledOutAt(prisonCode: String) = findByCode(prisonCode)?.isActivitiesRolledOut() == true
