package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ActivityMetricsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.AppointmentMetricsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateScheduledInstancesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ManageAllocationsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ManageAttendanceRecordsJob

// These endpoints are secured in the ingress rather than the app so that they can be called from
// within the namespace without requiring authentication

@RestController
@ProtectedByIngress
@RequestMapping("/job", produces = [MediaType.TEXT_PLAIN_VALUE])
class JobTriggerController(
  private val createScheduledInstancesJob: CreateScheduledInstancesJob,
  private val manageAttendanceRecordsJob: ManageAttendanceRecordsJob,
  private val manageAllocationsJob: ManageAllocationsJob,
  private val activityMetricsJob: ActivityMetricsJob,
  private val appointmentMetricsJob: AppointmentMetricsJob,
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

  @PostMapping(value = ["/manage-attendance-records"])
  @Operation(
    summary = "Trigger the job to manage attendance records in advance",
    description = "Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code.",
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  fun triggerManageAttendanceRecordsJob(
    @RequestParam(value = "withExpiry", required = false)
    @Parameter(description = "If true will run the attendance expiry process in addition to other features. Defaults to false.")
    withExpiry: Boolean = false,
  ): String {
    manageAttendanceRecordsJob.execute(withExpiry)
    return "Manage attendance records triggered"
  }

  @PostMapping(value = ["/manage-allocations"])
  @Operation(
    summary = "Trigger the job to manage allocations",
    description = """
        One or more operations to trigger for managing allocations.
        
        Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code.
    """,
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  fun triggerManageAllocationsJob(
    @RequestParam(value = "withActivate", required = false)
    @Parameter(description = "If true will run the activate pending allocations process. Defaults to false.")
    withActivate: Boolean = false,
    @RequestParam(value = "withDeallocate", required = false)
    @Parameter(description = "If true will run the deallocate allocations process. Defaults to false.")
    withDeallocate: Boolean = false,
  ): String {
    manageAllocationsJob.execute(withActivate = withActivate, withDeallocate = withDeallocate)

    return "Manage allocations triggered operations"
  }

  @PostMapping(value = ["/activities-metrics"])
  @Operation(
    summary = "Trigger the job to generate activity metrics",
    description = """Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code.""",
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  fun triggerActivityMetricsJob(): String {
    activityMetricsJob.execute()

    return "Activity metrics job triggered"
  }

  @PostMapping(value = ["/appointments-metrics"])
  @Operation(
    summary = "Trigger the job to generate appointments metrics",
    description = """Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code.""",
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun triggerAppointmentsMetricsJob(): String {
    appointmentMetricsJob.execute()

    return "Appointments metrics job triggered"
  }
}
