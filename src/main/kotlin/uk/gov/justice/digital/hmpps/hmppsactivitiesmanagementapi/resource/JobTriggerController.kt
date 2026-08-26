package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.runBlocking
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.FixLocationsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ManageAllocationsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ManageAttendanceRecordsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.PurposefulActivityReportsJob
import java.time.Clock
import java.time.LocalDate

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
  private val clock: Clock,
  private val purposefulActivityReportsJob: PurposefulActivityReportsJob,
  private val fixLocationsJob: FixLocationsJob,
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
    @RequestParam(value = "prisonCode", required = false)
    @Parameter(description = "If supplied will create attendance records for the given rolled out prison.")
    prisonCode: String? = null,
    @RequestParam(value = "date", required = false)
    @Parameter(description = "If supplied will create attendance records for the given date. Default to the current date.")
    date: LocalDate? = null,
    @RequestParam(value = "withExpiry", required = false)
    @Parameter(description = "If true will run the attendance expiry process in addition to other features. Defaults to false.")
    withExpiry: Boolean = false,
  ): String {
    manageAttendanceRecordsJob.execute(mayBePrisonCode = prisonCode, date = date ?: LocalDate.now(clock), withExpiry = withExpiry)
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
    @RequestParam(value = "withDeallocateEnding", required = false)
    @Parameter(description = "If true will run the deallocate allocations that are ending process. Defaults to false.")
    withDeallocateEnding: Boolean = false,
    @RequestParam(value = "withDeallocateExpiring", required = false)
    @Parameter(description = "If true will run the deallocate allocations that are expiring process. Defaults to false.")
    withDeallocateExpiring: Boolean = false,
    @RequestParam(value = "withFixAutoSuspended", required = false)
    @Parameter(description = "If true will attempt to unsuspend auto suspended prisoners who should have been received back into the prison already.")
    withFixAutoSuspended: Boolean = false,
  ): String {
    manageAllocationsJob.execute(
      withActivate = withActivate,
      withDeallocateEnding = withDeallocateEnding,
      withDeallocateExpiring = withDeallocateExpiring,
      withFixAutoSuspended = withFixAutoSuspended,
    )

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

  @PostMapping(value = ["/fix-locations"])
  @Operation(
    summary = "Trigger the job fix activity and appointment locations",
    description = """Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code.""",
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun triggerActivitiesLocationsJob(): String {
    fixLocationsJob.execute()

    return "Fix locations job triggered"
  }

  @PostMapping(value = ["/purposeful-activity-reports"])
  @Operation(
    summary = "Trigger the job to generate purposeful activity reports and upload to s3",
    description = """
      Generates 3 csv reports which are uploaded to an s3 bucket for prison performance reporting team to process for
      purposeful activity generation purposes and to display on the prison regime dashboard in the performance hub.

      Report 1) Details of attended purposeful-activity activities
      Report 2) Details of attended purposeful-activity appointments
      Report 3) Prison rollout table

      Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code.
      """,
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun triggerPurposefulActivityReportsJob(
    @RequestParam(value = "weekOffset", required = false, defaultValue = "1")
    @Parameter(description = "Report is calculated for the week up to the prior saturday. increase offset to generate reports for weeks prior to that")
    weekOffset: Int,
  ): String {
    runBlocking {
      purposefulActivityReportsJob.execute(weekOffset)
    }
    return "Purposeful Activity Reports job triggered"
  }
}
