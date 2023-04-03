package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment

@Repository
interface AppointmentRepository : JpaRepository<Appointment, Long> {
  @Query(
    value = "FROM Appointment a " +
      "JOIN  AppointmentOccurrence ao on a.appointmentId = ao.appointment.appointmentId " +
      "WHERE ao.appointmentOccurrenceId = :appointmentOccurrenceId",
  )
  fun findByAppointmentOccurrenceId(appointmentOccurrenceId: Long): Appointment?
}
