package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonRegime

@Repository
interface PrisonRegimeRepository : JpaRepository<PrisonRegime, Long> {
  fun findByPrisonCode(code: String): PrisonRegime?
}
