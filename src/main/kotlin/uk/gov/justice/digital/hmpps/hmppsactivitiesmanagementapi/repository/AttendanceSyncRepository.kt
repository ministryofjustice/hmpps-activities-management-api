package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceSync
import org.springframework.stereotype.Repository as RepositoryAnnotation

@RepositoryAnnotation
interface AttendanceSyncRepository : ReadOnlyRepository<AttendanceSync, Long> {
  fun findAllByAttendanceId(attendanceId: Long): AttendanceSync?
}
