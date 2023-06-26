package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocationSearch

@Repository
interface AppointmentOccurrenceAllocationSearchRepository : ReadOnlyRepository<AppointmentOccurrenceAllocationSearch, Long> {
  @Query(
    value = "FROM AppointmentOccurrenceAllocationSearch aoa " +
      "WHERE aoa.appointmentOccurrenceSearch.appointmentOccurrenceId IN :appointmentOccurrenceIds",
  )
  fun findByAppointmentOccurrenceIds(appointmentOccurrenceIds: List<Long>): List<AppointmentOccurrenceAllocationSearch>
}
