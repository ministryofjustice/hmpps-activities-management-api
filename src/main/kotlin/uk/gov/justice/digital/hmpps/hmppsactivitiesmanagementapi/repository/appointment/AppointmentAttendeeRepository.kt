package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentAttendee
import java.time.LocalDate

@Repository
interface AppointmentAttendeeRepository : JpaRepository<AppointmentAttendee, Long> {
  fun findByPrisonerNumber(prisonerNumber: String): List<AppointmentAttendee>

  @Query(value = "UPDATE AppointmentAttendee a SET a.prisonerNumber = :newNumber, a.bookingId = coalesce(:newBookingId, a.bookingId) WHERE a.prisonerNumber = :oldNumber")
  @Modifying
  fun mergeOldPrisonerNumberToNew(oldNumber: String, newNumber: String, newBookingId: Long?)

  @Query(value = "UPDATE AppointmentAttendee a SET a.bookingId = coalesce(:newBookingId, a.bookingId) WHERE a.prisonerNumber = :prisonerNumber")
  @Modifying
  fun mergePrisonerToNewBookingId(prisonerNumber: String, newBookingId: Long?)

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
