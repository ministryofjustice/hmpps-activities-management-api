package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence

@Repository
interface AppointmentOccurrenceRepository : JpaRepository<AppointmentOccurrence, Long> {
  fun findByAppointmentAndSequenceNumber(appointmentSeries: AppointmentSeries, sequenceNumber: Int): AppointmentOccurrence?
}
