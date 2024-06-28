package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum

interface AttendanceReasonRepository : JpaRepository<AttendanceReason, Long> {
  fun findByCode(code: AttendanceReasonEnum): AttendanceReason
}
