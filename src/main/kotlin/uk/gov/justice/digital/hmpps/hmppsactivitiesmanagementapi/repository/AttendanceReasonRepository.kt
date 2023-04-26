package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum

interface AttendanceReasonRepository : JpaRepository<AttendanceReason, Long> {
  fun findByCode(code: AttendanceReasonEnum): AttendanceReason
}
