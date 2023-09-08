package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendeeSearch

@Repository
interface AppointmentAttendeeSearchRepository : ReadOnlyRepository<AppointmentAttendeeSearch, Long> {
  @Query(
    value = "FROM AppointmentAttendeeSearch aas " +
      "WHERE aas.appointmentSearch.appointmentId IN :appointmentIds",
  )
  fun findByAppointmentIds(appointmentIds: List<Long>): List<AppointmentAttendeeSearch>
}
