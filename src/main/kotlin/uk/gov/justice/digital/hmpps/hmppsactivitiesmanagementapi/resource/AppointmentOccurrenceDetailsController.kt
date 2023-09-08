package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentDetailsService

@RestController
@RequestMapping("/appointment-occurrence-details", produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentOccurrenceDetailsController(
  private val appointmentDetailsService: AppointmentDetailsService,
) {
  @GetMapping(value = ["/{appointmentOccurrenceId}"])
  @ResponseBody
  @Operation(
    summary = "Gets the appointment occurrence details for display purposes identified by the appointment occurrence's id",
    description = "Returns the displayable details of an appointment occurrence by its unique identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment Occurrence found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentOccurrenceDetails::class),
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
        description = "The appointment occurrence for this ID was not found.",
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
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getAppointmentOccurrenceDetailsById(@PathVariable("appointmentOccurrenceId") appointmentOccurrenceId: Long): AppointmentOccurrenceDetails =
    appointmentDetailsService.getAppointmentDetailsById(appointmentOccurrenceId)
}
