package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.BulkAppointmentDetailsService

@RestController
@RequestMapping("/bulk-appointment-details", produces = [MediaType.APPLICATION_JSON_VALUE])
class BulkAppointmentDetailsController(
  private val bulkAppointmentDetailsService: BulkAppointmentDetailsService,
) {
  @GetMapping(value = ["/{bulkAppointmentId}"])
  @ResponseBody
  @Operation(
    summary = "Bulk create a set of appointments",
    description =
    """
    Create a list of appointments and allocate the supplied prisoner or prisoners to them.
    Does not require any specific roles
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Bulk appointment found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = BulkAppointmentDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The bulk appointment for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getBulkAppointmentDetailsById(@PathVariable("bulkAppointmentId") bulkAppointmentId: Long): BulkAppointmentDetails =
    bulkAppointmentDetailsService.getBulkAppointmentDetailsById(bulkAppointmentId)
}
