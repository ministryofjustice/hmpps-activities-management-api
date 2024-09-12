package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonRegime

@Repository
interface PrisonRegimeRepository : JpaRepository<PrisonRegime, Long> {
  fun findByPrisonCode(code: String): List<PrisonRegime>
  fun deleteByPrisonCode(code: String)
}
