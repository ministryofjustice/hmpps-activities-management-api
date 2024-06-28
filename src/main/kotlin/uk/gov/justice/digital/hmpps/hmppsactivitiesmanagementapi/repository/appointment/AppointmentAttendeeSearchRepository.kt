package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment

import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentAttendeeSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ReadOnlyRepository

@Repository
interface AppointmentAttendeeSearchRepository : ReadOnlyRepository<AppointmentAttendeeSearch, Long> {
  @Query(
    value = "FROM AppointmentAttendeeSearch aas " +
      "WHERE aas.appointmentSearch.appointmentId IN :appointmentIds",
  )
  fun findByAppointmentIds(appointmentIds: List<Long>): List<AppointmentAttendeeSearch>
}
