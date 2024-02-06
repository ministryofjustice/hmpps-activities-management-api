package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarAllocation
import java.time.LocalDate

@Repository
interface SarAllocationRepository : ReadOnlyRepository<SarAllocation, Long> {
  @Query(
    """
    SELECT sa FROM SarAllocation sa
     WHERE sa.prisonerNumber = :prisonerNumber
       AND sa.startDate >= :startDate
       AND (sa.endDate is null or sa.endDate <= :endDate)
    """,
  )
  fun findBy(@Param("prisonerNumber") prisonerNumber: String, @Param("startDate") startDate: LocalDate, @Param("endDate") endDate: LocalDate): List<SarAllocation>
}

@Component
class SarRepository(
  private val allocation: SarAllocationRepository,
) {
  fun findAllocationsBy(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) = allocation.findBy(prisonerNumber, fromDate, toDate)
}
