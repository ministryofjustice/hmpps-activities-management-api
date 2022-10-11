package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison

@Service
interface RolloutPrisonRepository : JpaRepository<RolloutPrison, Long> {
  fun findByCode(code: String): RolloutPrison?
}
