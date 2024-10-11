package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceCreate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand
import java.time.LocalDate
import org.springframework.stereotype.Repository as RepositoryAnnotation

@RepositoryAnnotation
interface AttendanceCreateRepository : ReadOnlyRepository<AttendanceCreate, Long> {

  @Query(
    value = """
      SELECT ac 
      FROM AttendanceCreate ac
      WHERE ac.prisonCode = :prisonCode
      AND ac.sessionDate = :sessionDate
      AND ac.allocStart <= :sessionDate
      AND (ac.allocEnd is null OR ac.allocEnd >= :sessionDate)
    """
  )
  fun findBy(prisonCode: String, sessionDate: LocalDate): List<AttendanceCreate>

  @Query(
    value = """
      SELECT ppb 
      FROM PrisonPayBand ppb 
      WHERE ppb.prisonPayBandId = :prisonPayBandId 
    """
  )
  fun findPayBandById(prisonPayBandId: Long): PrisonPayBand
}
