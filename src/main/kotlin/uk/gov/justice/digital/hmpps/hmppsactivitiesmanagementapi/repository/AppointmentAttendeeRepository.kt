package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee
import java.time.LocalDate

@Repository
interface AppointmentAttendeeRepository : JpaRepository<AppointmentAttendee, Long> {
  fun findByPrisonerNumber(prisonerNumber: String): List<AppointmentAttendee>

  @Query(value = "UPDATE AppointmentAttendee a SET a.prisonerNumber = :newNumber WHERE a.prisonerNumber = :oldNumber")
  @Modifying
  fun mergeOffender(oldNumber: String, newNumber: String)

  @Query(
    """
    SELECT aa FROM AppointmentAttendee aa
    JOIN Appointment a ON a.appointmentId = aa.appointment.appointmentId
    WHERE a.prisonCode = :prisonCode
    AND a.categoryCode = :categoryCode
    AND cast(aa.attendanceRecordedTime as LocalDate) = :recordedDate
    """,
  )
  fun findByPrisonCodeAndCategoryAndRecordedDate(
    prisonCode: String,
    categoryCode: String,
    recordedDate: LocalDate,
  ): List<AppointmentAttendee>
}
