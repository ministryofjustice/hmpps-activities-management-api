package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee

@Repository
interface AppointmentAttendeeRepository : JpaRepository<AppointmentAttendee, Long> {
  fun findByPrisonerNumber(prisonerNumber: String): List<AppointmentAttendee>

  @Query(value = "UPDATE AppointmentAttendee a SET a.prisonerNumber = :newNumber WHERE a.prisonerNumber = :oldNumber")
  @Modifying
  fun mergeOffender(oldNumber: String, newNumber: String)
}
