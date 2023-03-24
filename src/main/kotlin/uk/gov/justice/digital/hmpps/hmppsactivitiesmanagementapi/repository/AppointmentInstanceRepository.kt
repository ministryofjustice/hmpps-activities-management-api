package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Repository as RepositoryAnnotation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import java.time.LocalDate
import java.time.LocalTime

@RepositoryAnnotation
interface AppointmentInstanceRepository : Repository<AppointmentInstance, Long> {
  @Query(
    value =
    "FROM AppointmentInstance ai " +
      "WHERE ai.bookingId = :bookingId" +
      "  AND ai.appointmentDate BETWEEN :startDate AND :endDate",
  )
  fun findByBookingIdAndDateRange(bookingId: Long, startDate: LocalDate, endDate: LocalDate): List<AppointmentInstance>

  @Query(
    value =
    "FROM AppointmentInstance ai " +
      "WHERE ai.prisonCode = :prisonCode" +
      "  AND ai.prisonerNumber IN :prisonerNumbers" +
      "  AND ai.appointmentDate = :date" +
      "  AND ai.startTime BETWEEN :earliestStartTime AND :latestStartTime",
  )
  fun findByPrisonCodeAndPrisonerNumberAndDateAndTime(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    earliestStartTime: LocalTime,
    latestStartTime: LocalTime,
  ): List<AppointmentInstance>
}
