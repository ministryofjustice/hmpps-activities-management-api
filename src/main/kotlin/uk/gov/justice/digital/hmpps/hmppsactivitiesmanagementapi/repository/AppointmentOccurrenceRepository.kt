package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries

@Repository
interface AppointmentOccurrenceRepository : JpaRepository<Appointment, Long> {
  fun findByAppointmentSeriesAndSequenceNumber(appointmentSeries: AppointmentSeries, sequenceNumber: Int): Appointment?
}
