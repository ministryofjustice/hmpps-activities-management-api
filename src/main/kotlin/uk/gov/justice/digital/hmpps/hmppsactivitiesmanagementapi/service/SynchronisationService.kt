package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceSync
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceSyncRepository

@Service
class SynchronisationService(private val repository: AttendanceSyncRepository) {
  fun getAttendanceSync(attendanceId: Long): AttendanceSync =
    repository.findById(attendanceId)
      .orElseThrow { EntityNotFoundException("Attendance sync not found: $attendanceId") }
      .toModel()
}
