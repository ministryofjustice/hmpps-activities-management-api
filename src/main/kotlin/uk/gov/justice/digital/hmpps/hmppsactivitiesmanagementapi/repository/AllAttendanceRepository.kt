package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AllAttendance
import java.time.LocalDate
import org.springframework.stereotype.Repository as RepositoryAnnotation

@RepositoryAnnotation
interface AllAttendanceRepository : ReadOnlyRepository<AllAttendance, Long> {
  fun findByPrisonCodeAndSessionDate(prisonCode: String, sessionDate: LocalDate): List<AllAttendance>
}
