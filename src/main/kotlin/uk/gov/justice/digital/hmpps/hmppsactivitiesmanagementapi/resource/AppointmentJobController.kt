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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ManageAppointmentAttendeesJob

// These endpoints are secured in the ingress rather than the app so that they can be called from
// within the namespace without requiring authentication

@RestController
@ProtectedByIngress
@RequestMapping("/job/appointments", produces = [MediaType.TEXT_PLAIN_VALUE])
class AppointmentJobController(
  private val manageAppointmentAttendeesJob: ManageAppointmentAttendeesJob,
) {
  @PostMapping(value = ["/manage-attendees"])
  @Operation(
    summary = "Starts a job to manage appointment attendees",
    description = """
      Job will retrieve all future appointments starting within the number of days defined by the days after now parameter.
      It will retrieve the attendees for these appointments and retrieve the associated prisoner records. The status, location and
      any other pertinent information for these prisoners will be used to determine whether the attendee records should be removed.
      
      Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code.
    """,
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun triggerManageAllocationsJob(
    @RequestParam(value = "daysAfterNow", required = true)
    @Parameter(description = "The number of days into the future to manage appointments up to a maximum of 60. The attendees for future appointments starting on those days this will be managed.")
    daysAfterNow: Long,
  ): String {
    manageAppointmentAttendeesJob.execute(daysAfterNow)

    return "Manage appointment attendees job started"
  }
}
