package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AdvanceAttendance

interface AdvanceAttendanceRepository : JpaRepository<AdvanceAttendance, Long>
