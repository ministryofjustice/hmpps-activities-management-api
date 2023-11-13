package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee

@Repository
interface AppointmentAttendeeRepository : JpaRepository<AppointmentAttendee, Long> {
  fun findByPrisonerNumber(prisonerNumber: String): List<AppointmentAttendee>
}
