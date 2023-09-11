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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentSeriesService
import java.security.Principal

@RestController
@RequestMapping("/appointment-series", produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentSeriesController(
  private val appointmentSeriesService: AppointmentSeriesService,
) {
  @GetMapping(value = ["/{appointmentSeriesId}"])
  @ResponseBody
  @Operation(
    summary = "Get an appointment series by its id",
    description = "Returns an appointment series with its properties and references to NOMIS by its unique identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment series found",
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
        description = "The appointment series for this id was not found.",
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
  fun getAppointmentSeriesById(@PathVariable("appointmentSeriesId") appointmentSeriesId: Long): AppointmentSeries =
    appointmentSeriesService.getAppointmentSeriesById(appointmentSeriesId)

  @GetMapping(value = ["/{appointmentSeriesId}/details"])
  @ResponseBody
  @Operation(
    summary = "Get the details of an appointment series for display purposes by its id",
    description = "Returns the displayable details of an appointment series by its unique identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment series found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentSeriesDetails::class),
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
        description = "The appointment series for this id was not found.",
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
  fun getAppointmentDetailsById(@PathVariable("appointmentSeriesId") appointmentSeriesId: Long): AppointmentSeriesDetails =
    appointmentSeriesService.getAppointmentSeriesDetailsById(appointmentSeriesId)

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  @Operation(
    summary = "Create an appointment series with one or more appointments",
    description =
    """
    Create an appointment series with one or more appointments and add the supplied prisoner or prisoners as appointment attendees.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The appointment series was created.",
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
  fun createAppointmentSeries(
    principal: Principal,
    @Valid
    @RequestBody
    @Parameter(
      description = "The create request with the new appointment series details",
      required = true,
    )
    request: AppointmentSeriesCreateRequest,
  ): AppointmentSeries = appointmentSeriesService.createAppointmentSeries(request, principal)
}
