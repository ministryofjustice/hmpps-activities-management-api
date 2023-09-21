package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.stereotype.Repository as RepositoryAnnotation

@RepositoryAnnotation
interface AppointmentInstanceRepository : ReadOnlyRepository<AppointmentInstance, Long> {
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

  @Query(
    value =
    "FROM AppointmentInstance ai " +
      "WHERE ai.prisonCode = :prisonCode" +
      "  AND ai.prisonerNumber = :prisonerNumber" +
      "  AND ai.appointmentDate >= CURRENT_DATE " +
      "  AND ((ai.appointmentDate > CURRENT_DATE) OR ai.startTime > CURRENT_TIME)",
  )
  fun findByPrisonCodeAndPrisonerNumberFromNow(
    prisonCode: String,
    prisonerNumber: String,
  ): List<AppointmentInstance>

  @Query(
    value =
    "FROM AppointmentInstance ai " +
      "WHERE ai.prisonCode = :prisonCode" +
      "  AND ai.internalLocationId IN :internalLocationIds" +
      "  AND ai.appointmentDate = :date" +
      "  AND ai.startTime BETWEEN :earliestStartTime AND :latestStartTime",
  )
  fun findByPrisonCodeAndInternalLocationIdsAndDateAndTime(
    prisonCode: String,
    internalLocationIds: Set<Long>,
    date: LocalDate,
    earliestStartTime: LocalTime,
    latestStartTime: LocalTime,
  ): List<AppointmentInstance>
}
