package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceSync
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceSyncRepository

@Service
@Transactional(readOnly = true)
class SynchronisationService(private val repository: AttendanceSyncRepository) {
  fun findAttendanceSync(attendanceId: Long): AttendanceSync? =
    repository.findAllByAttendanceId(attendanceId)
      .maxByOrNull { it.allocationStartDate }
      ?.toModel()
}
