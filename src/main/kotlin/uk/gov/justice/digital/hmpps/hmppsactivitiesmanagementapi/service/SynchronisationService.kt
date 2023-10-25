package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllocationReconciliationResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReconciliationResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceSync
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceSyncRepository
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class SynchronisationService(
  private val attendanceSyncRepository: AttendanceSyncRepository,
  private val allocationRepository: AllocationRepository,
  private val attendanceRepository: AttendanceRepository,
) {
  fun findAttendanceSync(attendanceId: Long): AttendanceSync? =
    attendanceSyncRepository.findAllByAttendanceId(attendanceId)
      ?.toModel()

  fun findActiveAllocationsSummary(prisonCode: String): AllocationReconciliationResponse =
    allocationRepository.findBookingAllocationCountsByPrisonAndPrisonerStatus(prisonCode, PrisonerStatus.ACTIVE)
      .let {
        AllocationReconciliationResponse(
          prisonCode = prisonCode,
          bookings = it,
        )
      }

  fun findAttendancePaidSummary(prisonCode: String, date: LocalDate): AttendanceReconciliationResponse =
    attendanceRepository.findBookingPaidAttendanceCountsByPrisonAndDate(prisonCode, date)
      .let {
        AttendanceReconciliationResponse(
          prisonCode = prisonCode,
          date = date,
          bookings = it,
        )
      }
}
