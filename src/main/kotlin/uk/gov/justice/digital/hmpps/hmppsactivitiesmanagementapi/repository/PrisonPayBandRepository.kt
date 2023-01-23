package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonPayBand

@Repository
interface PrisonPayBandRepository : JpaRepository<PrisonPayBand, Long> {
  fun findByPrisonCode(code: String): List<PrisonPayBand>
}
