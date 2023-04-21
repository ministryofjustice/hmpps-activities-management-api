package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceSearch
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.stereotype.Repository as RepositoryAnnotation

@RepositoryAnnotation
interface AppointmentOccurrenceSearchRepository : ReadOnlyRepository<AppointmentOccurrenceSearch, Long> {
  @Query(
    value =
    "FROM AppointmentOccurrenceSearch aos " +
      "WHERE aos.prisonCode = :prisonCode" +
      "  AND (:categoryCode IS NULL OR aos.categoryCode = :categoryCode)" +
      "  AND (:internalLocationId IS NULL OR aos.internalLocationId = :internalLocationId)" +
      "  AND (:startDate IS NULL OR aos.startDate = :startDate)" +
      "  AND (:earliestStartTime IS NULL OR :latestStartTime IS NULL OR aos.startTime BETWEEN :earliestStartTime AND :latestStartTime)",
  )
  fun find(
    prisonCode: String,
    categoryCode: String? = null,
    internalLocationId: Long? = null,
    startDate: LocalDate? = null,
    earliestStartTime: LocalTime? = null,
    latestStartTime: LocalTime? = null,
  ): List<AppointmentOccurrenceSearch>
}
