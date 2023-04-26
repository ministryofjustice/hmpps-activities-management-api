package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AllAttendanceSummary
import java.time.LocalDate
import org.springframework.stereotype.Repository as RepositoryAnnotation

@RepositoryAnnotation
interface AllAttendanceSummaryRepository : ReadOnlyRepository<AllAttendanceSummary, Long> {
  fun findBySessionDate(sessionDate: LocalDate): List<AllAttendanceSummary>
}
