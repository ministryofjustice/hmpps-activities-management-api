package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventPriority

@Repository
interface EventPriorityRepository : JpaRepository<EventPriority, Long> {
  fun findByPrisonCode(code: String): List<EventPriority>
}
