package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DataFix

@Repository
interface DataFixRepository : JpaRepository<DataFix, Long> {
  fun findByActivityScheduleId(activityScheduleId: Long): List<DataFix>
}
