package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository

@Service
@Transactional(readOnly = true)
class AttendanceReasonService(
  private val attendanceReasonRepository: AttendanceReasonRepository,
) {
  fun getAll() =
    attendanceReasonRepository.findAll().toModel()
}
