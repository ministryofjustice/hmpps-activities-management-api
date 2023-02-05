package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCategory

@Repository
interface AppointmentCategoryRepository : JpaRepository<AppointmentCategory, Long> {
  /**
   * Custom query rather than derived from method name as the latter doesn't support NULLS LAST.
   * PostgreSQL defaults to nulls last but the H2 in memory database used for integration tests orders nulls first.
   * In the unlikely event that the database was changed from PostgreSQL, this will maintain correct sorting.
   */
  @Query(value = "FROM AppointmentCategory ORDER BY displayOrder NULLS LAST, description")
  fun findAllOrdered(): List<AppointmentCategory>
}
