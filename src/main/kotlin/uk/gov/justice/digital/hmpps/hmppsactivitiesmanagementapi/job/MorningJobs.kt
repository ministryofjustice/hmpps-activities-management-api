package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendanceOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAttendancesService

@Component
class MorningJobs(
  private val service: ManageAllocationsService,
  private val attendancesService: ManageAttendancesService,
  private val jobRunner: SafeJobRunner,
) {

  @Async("asyncExecutor")
  fun execute() {
    jobRunner.runDependentJobs(
      JobDefinition(JobType.ALLOCATE) { service.allocations(AllocationOperation.STARTING_TODAY) },
      JobDefinition(JobType.ATTENDANCE_CREATE) { attendancesService.attendances(AttendanceOperation.CREATE) },
    )
  }
}
