package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentSeriesService
import java.security.Principal

@RestController
@RequestMapping("/appointments", produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentController(
  private val appointmentSeriesService: AppointmentSeriesService,
) {
  @GetMapping(value = ["/{appointmentId}"])
  @ResponseBody
  @Operation(
    summary = "Get an appointment by its id",
    description = "Returns an appointment and its details by its unique identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentSeries::class),
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
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getAppointmentById(@PathVariable("appointmentId") appointmentId: Long): AppointmentSeries =
    appointmentSeriesService.getAppointmentSeriesById(appointmentId)

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  @Operation(
    summary = "Create an appointment or series of appointment occurrences",
    description =
    """
    Create an appointment or series of appointment occurrences and allocate the supplied prisoner or prisoners to them.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The appointment or series of appointment occurrences was created.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentSeries::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
    ],
  )
  @CaseloadHeader
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun createAppointment(
    principal: Principal,
    @Valid
    @RequestBody
    @Parameter(
      description = "The create request with the new appointment or series of appointment occurrences details",
      required = true,
    )
    request: AppointmentSeriesCreateRequest,
  ): AppointmentSeries = appointmentSeriesService.createAppointmentSeries(request, principal)
}
