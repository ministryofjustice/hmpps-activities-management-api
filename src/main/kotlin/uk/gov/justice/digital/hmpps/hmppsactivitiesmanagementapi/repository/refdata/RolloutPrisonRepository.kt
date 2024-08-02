package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.RolloutPrison

@Repository
interface RolloutPrisonRepository : JpaRepository<RolloutPrison, Long> {
  fun findByCode(code: String): RolloutPrison?
}
