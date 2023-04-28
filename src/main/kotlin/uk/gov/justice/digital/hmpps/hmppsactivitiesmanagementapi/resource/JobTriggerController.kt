package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateAttendanceRecordsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateScheduledInstancesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ManageAllocationsJob

// These endpoints are secured in the ingress rather than the app so that they can be called from
// within the namespace without requiring authentication

@RestController
@RequestMapping("/job", produces = [MediaType.TEXT_PLAIN_VALUE])
class JobTriggerController(
  private val createScheduledInstancesJob: CreateScheduledInstancesJob,
  private val createAttendanceRecordsJob: CreateAttendanceRecordsJob,
  private val manageAllocationsJob: ManageAllocationsJob,
) {

  @PostMapping(value = ["/create-scheduled-instances"])
  @Operation(
    summary = "Trigger the job to create the scheduled instances in advance for the active schedules on activities",
    description = "Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code.",
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  fun triggerCreateScheduledInstancesJob(): String {
    createScheduledInstancesJob.execute()
    return "Create scheduled instances triggered"
  }

  @PostMapping(value = ["/create-attendance-records"])
  @Operation(
    summary = "Trigger the job to create attendance records in advance",
    description = "Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code.",
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  fun triggerCreateAttendanceRecordsJob(): String {
    createAttendanceRecordsJob.execute()
    return "Create attendance records triggered"
  }

  @PostMapping(value = ["/manage-allocations"])
  @Operation(
    summary = "Trigger the job to manage allocations",
    description = "Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code.",
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  fun triggerManageAllocationsJob(): String {
    manageAllocationsJob.execute()
    return "Manage allocations triggered"
  }
}
