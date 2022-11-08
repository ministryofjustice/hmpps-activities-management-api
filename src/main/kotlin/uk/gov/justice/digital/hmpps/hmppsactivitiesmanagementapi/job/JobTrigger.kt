package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

// These endpoints are secured in the ingress rather than the app so that they can be called from
// within the namespace without requiring authentication

@RestController
@RequestMapping("/job", produces = [MediaType.APPLICATION_JSON_VALUE])
class JobTrigger(
  private val createActivitySessionsJob: CreateActivitySessionsJob,
  private val createAttendanceRecordsJob: CreateAttendanceRecordsJob
) {

  @PostMapping(value = ["/create-activity-sessions"])
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  fun triggerCreateActivitySessionsJob(): String {
    createActivitySessionsJob.execute()
    return "Activity sessions scheduled"
  }

  @PostMapping(value = ["/create-attendance-records"])
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  fun triggerCreateAttendanceRecordsJob(): String {
    createAttendanceRecordsJob.execute()
    return "Create attendance records triggered"
  }
}
