package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarWaitingList
import java.time.LocalDate

@Repository
interface SarAllocationRepository : ReadOnlyRepository<SarAllocation, Long> {
  fun findByPrisonerNumberAndCreatedDateBetween(@Param("prisonerNumber") prisonerNumber: String, @Param("fromDate") fromDat: LocalDate, @Param("toDate") toDate: LocalDate): List<SarAllocation>
}

@Repository
interface SarWaitingListRepository : ReadOnlyRepository<SarWaitingList, Long> {
  fun findByPrisonerNumberAndCreatedDateBetween(@Param("prisonerNumber") prisonerNumber: String, @Param("fromDate") fromDat: LocalDate, @Param("toDate") toDate: LocalDate): List<SarWaitingList>
}

@Component
class SarRepository(
  private val allocation: SarAllocationRepository,
  private val waitingList: SarWaitingListRepository,
) {
  fun findAllocationsBy(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) = allocation.findByPrisonerNumberAndCreatedDateBetween(prisonerNumber, fromDate, toDate)

  fun findWaitingListsBy(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) = waitingList.findByPrisonerNumberAndCreatedDateBetween(prisonerNumber, fromDate, toDate)
}
