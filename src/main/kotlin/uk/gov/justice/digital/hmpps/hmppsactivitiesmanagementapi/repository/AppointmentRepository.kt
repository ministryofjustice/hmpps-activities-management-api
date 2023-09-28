package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import java.time.LocalDate

@Repository
interface AppointmentRepository : JpaRepository<Appointment, Long> {
  fun findByAppointmentSeriesAndSequenceNumber(appointmentSeries: AppointmentSeries, sequenceNumber: Int): Appointment?

  @Query(
    value =
    "FROM Appointment a " +
      "WHERE a.prisonCode = :prisonCode" +
      " AND a.categoryCode = :categoryCode" +
      "  AND a.startDate = :startDate",
  )
  fun findByPrisonCodeAndCategoryCodeAndDate(prisonCode: String, categoryCode: String, startDate: LocalDate): List<Appointment>
}
