package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeries
import java.time.LocalDate
import java.time.LocalTime

@Repository
interface AppointmentRepository : JpaRepository<Appointment, Long> {
  fun findAllByPrisonCodeAndStartDate(prisonCode: String, startDate: LocalDate): List<Appointment>
  fun findByAppointmentSeriesAndSequenceNumber(appointmentSeries: AppointmentSeries, sequenceNumber: Int): Appointment?

  @Query(
    value =
    "SELECT * FROM appointment a " +
      "WHERE a.prison_code = :prisonCode" +
      " AND a.category_code = :categoryCode" +
      "  AND a.start_date = :startDate",
    nativeQuery = true,
  )
  fun findByPrisonCodeAndCategoryCodeAndDate(prisonCode: String, categoryCode: String, startDate: LocalDate): List<Appointment>

  @Query(
    value = """
      SELECT 
      aa.prisoner_number,
      aa.booking_id,
      a.appointment_id,
      aa.appointment_attendee_id,
      a.start_date,
      a.start_time,
      a.end_time,
      et.code AS event_tier,
      a.category_code,
      a.custom_name,
      aa.attended
      FROM appointment a
      JOIN appointment_series aps ON aps.appointment_series_id = a.appointment_series_id
      JOIN appointment_attendee aa ON aa.appointment_id = a.appointment_id
      LEFT JOIN event_tier et ON et.event_tier_id = a.appointment_tier_id
      LEFT JOIN event_organiser eo ON eo.event_organiser_id = aps.appointment_organiser_id
      WHERE a.start_date = :date
        AND a.prison_code = :prisonCode
        AND aa.removal_reason_id IS NULL
        AND NOT a.is_deleted
        AND (:categoryCode IS NULL OR a.category_code = :categoryCode)
        AND (:customName IS NULL OR LOWER(a.custom_name) = LOWER(:customName))
        AND (:prisonerNumber IS NULL OR aa.prisoner_number = :prisonerNumber)
        AND (:organiserCode IS NULL OR eo.code = :organiserCode)
        AND ((NOT :isCancelled AND a.cancellation_reason_id IS NULL) OR (:isCancelled AND a.cancellation_reason_id IS NOT NULL))
    """,
    nativeQuery = true,
  )
  fun getAppointmentsWithAttendees(
    @Param("prisonCode") prisonCode: String,
    @Param("date") date: LocalDate,
    @Param("categoryCode") categoryCode: String?,
    @Param("customName") customName: String?,
    @Param("prisonerNumber") prisonerNumber: String?,
    @Param("isCancelled") isCancelled: Boolean,
    @Param("organiserCode") organiserCode: String?,
  ): List<AppointmentAndAttendee>
}

interface AppointmentAndAttendee {
  fun getPrisonerNumber(): String
  fun getBookingId(): Long
  fun getAppointmentId(): Long
  fun getAppointmentAttendeeId(): Long
  fun getStartDate(): LocalDate
  fun getStartTime(): LocalTime
  fun getEndTime(): LocalTime
  fun getEventTier(): String?
  fun getCategoryCode(): String
  fun getCustomName(): String?
  fun getAttended(): Boolean?
}
