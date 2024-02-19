package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarWaitingList
import java.time.LocalDate

@Repository
interface SarAllocationRepository : ReadOnlyRepository<SarAllocation, Long> {
  fun findByPrisonerNumberAndCreatedDateBetween(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate): List<SarAllocation>
}

@Repository
interface SarWaitingListRepository : ReadOnlyRepository<SarWaitingList, Long> {
  fun findByPrisonerNumberAndCreatedDateBetween(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate): List<SarWaitingList>
}

@Repository
interface SarAppointmentRepository : ReadOnlyRepository<SarAppointment, Long> {
  fun findByPrisonerNumberAndCreatedDateBetween(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate): List<SarAppointment>
}

@Component
class SarRepository(
  private val allocation: SarAllocationRepository,
  private val waitingList: SarWaitingListRepository,
  private val appointment: SarAppointmentRepository,
) {
  fun findAllocationsBy(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) = allocation.findByPrisonerNumberAndCreatedDateBetween(prisonerNumber, fromDate, toDate.plusDays(1))

  fun findWaitingListsBy(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) = waitingList.findByPrisonerNumberAndCreatedDateBetween(prisonerNumber, fromDate, toDate.plusDays(1))

  fun findAppointmentsBy(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) = appointment.findByPrisonerNumberAndCreatedDateBetween(prisonerNumber, fromDate, toDate.plusDays(1))
}
