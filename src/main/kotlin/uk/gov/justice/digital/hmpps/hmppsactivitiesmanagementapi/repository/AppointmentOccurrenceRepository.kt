package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment

@Repository
interface AppointmentOccurrenceRepository : JpaRepository<Appointment, Long> {
  fun findByAppointmentAndSequenceNumber(appointmentSeries: AppointmentSeries, sequenceNumber: Int): Appointment?
}
