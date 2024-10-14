package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceCreationData
import java.time.LocalDate
import org.springframework.stereotype.Repository as RepositoryAnnotation

@RepositoryAnnotation
interface AttendanceCreationDataRepository : ReadOnlyRepository<AttendanceCreationData, Long> {

  @Query(
    value = """
      SELECT ac 
      FROM AttendanceCreationData ac
      WHERE ac.prisonCode = :prisonCode
      AND ac.sessionDate = :sessionDate
      AND ac.allocStart <= :sessionDate
      AND (ac.allocEnd is null OR ac.allocEnd >= :sessionDate)
    """,
  )
  fun findBy(prisonCode: String, sessionDate: LocalDate): List<AttendanceCreationData>
}
