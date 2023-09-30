package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import java.time.LocalDate

@Repository
interface AppointmentRepository : JpaRepository<Appointment, Long> {
  fun findAllByStartDate(startDate: LocalDate): List<Appointment>
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
}
