package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceSearch
import org.springframework.stereotype.Repository as RepositoryAnnotation

@RepositoryAnnotation
interface AppointmentOccurrenceSearchRepository : ReadOnlyRepository<AppointmentOccurrenceSearch, Long> {
  fun findByPrisonCode(prisonCode: String): List<AppointmentOccurrenceSearch>
}
