package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendeeSearch

@Repository
interface AppointmentOccurrenceAllocationSearchRepository : ReadOnlyRepository<AppointmentAttendeeSearch, Long> {
  @Query(
    value = "FROM AppointmentAttendeeSearch aoa " +
      "WHERE aoa.appointmentSearch.appointmentId IN :appointmentOccurrenceIds",
  )
  fun findByAppointmentOccurrenceIds(appointmentOccurrenceIds: List<Long>): List<AppointmentAttendeeSearch>
}
