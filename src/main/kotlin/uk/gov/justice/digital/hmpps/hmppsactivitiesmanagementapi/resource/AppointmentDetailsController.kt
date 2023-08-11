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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentDetailsService

@RestController
@RequestMapping("/appointment-details", produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentDetailsController(
  private val appointmentDetailsService: AppointmentDetailsService,
) {
  @GetMapping(value = ["/{appointmentId}"])
  @ResponseBody
  @Operation(
    summary = "Gets the top level appointment details for display purposes identified by the appointment's id",
    description = "Returns the displayable details of an appointment by its unique identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentDetails::class),
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
        description = "The appointment for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @CaseloadHeader
  fun getAppointmentDetailsById(@PathVariable("appointmentId") appointmentId: Long): AppointmentDetails =
    appointmentDetailsService.getAppointmentDetailsById(appointmentId)
}
