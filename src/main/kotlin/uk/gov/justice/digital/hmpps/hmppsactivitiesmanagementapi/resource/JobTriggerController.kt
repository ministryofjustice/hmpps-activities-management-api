package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateActivitySessionsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateAttendanceRecordsJob

// These endpoints are secured in the ingress rather than the app so that they can be called from
// within the namespace without requiring authentication

@RestController
@RequestMapping("/job", produces = [MediaType.APPLICATION_JSON_VALUE])
class JobTriggerController(
  private val createActivitySessionsJob: CreateActivitySessionsJob,
  private val createAttendanceRecordsJob: CreateAttendanceRecordsJob
) {

  @PostMapping(value = ["/create-activity-sessions"])
  @Operation(
    summary = "Trigger the job to create activity sessions in advance",
    description = "Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code."
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  fun triggerCreateActivitySessionsJob(): String {
    createActivitySessionsJob.execute()
    return "Activity sessions scheduled"
  }

  @PostMapping(value = ["/create-attendance-records"])
  @Operation(
    summary = "Trigger the job to create attendance records in advance",
    description = "Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code."
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  fun triggerCreateAttendanceRecordsJob(): String {
    createAttendanceRecordsJob.execute()
    return "Create attendance records triggered"
  }
}
