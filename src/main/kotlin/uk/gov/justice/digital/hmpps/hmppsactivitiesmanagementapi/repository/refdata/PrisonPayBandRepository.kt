package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand

@Repository
interface PrisonPayBandRepository : JpaRepository<PrisonPayBand, Long> {
  fun findByPrisonCode(code: String): List<PrisonPayBand>
}
