package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentSearchService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentService
import java.security.Principal

@RestController
@RequestMapping("/appointments", produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentController(
  private val appointmentService: AppointmentService,
  private val appointmentSearchService: AppointmentSearchService,
) {
  @GetMapping(value = ["/{appointmentId}"])
  @ResponseBody
  @Operation(
    summary = "Get an appointment by its id",
    description = "Returns an appointment with its properties and references to NOMIS by its unique identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Appointment::class),
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
        description = "The appointment for this id was not found.",
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
  fun getAppointmentById(@PathVariable("appointmentId") appointmentId: Long): Appointment =
    appointmentService.getAppointmentById(appointmentId)

  @GetMapping(value = ["/{appointmentId}/details"])
  @ResponseBody
  @Operation(
    summary = "Get the details of an appointment for display purposes by its id",
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
        description = "The appointment for this id was not found.",
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
  fun getAppointmentDetailsById(@PathVariable("appointmentId") appointmentId: Long): AppointmentDetails =
    appointmentService.getAppointmentDetailsById(appointmentId)

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PatchMapping(value = ["/{appointmentId}"])
  @Operation(
    summary = "Update an appointment or series of appointments",
    description =
    """
    Update an appointment or series of appointments based on the applyTo property.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "The appointment or series of appointments was updated.",
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
      ApiResponse(
        responseCode = "404",
        description = "The appointment for this id was not found.",
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
  fun updateAppointment(
    @PathVariable("appointmentId") appointmentId: Long,
    @Valid
    @RequestBody
    @Parameter(
      description = "The update request with the new appointment details and how to apply the update",
      required = true,
    )
    request: AppointmentUpdateRequest,
    principal: Principal,
  ): AppointmentSeries = appointmentService.updateAppointment(appointmentId, request, principal)

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PutMapping(value = ["/{appointmentId}/cancel"])
  @Operation(
    summary = "Cancel an appointment or series of appointments",
    description =
    """
    Cancel an appointment or series of appointments based on the applyTo property.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "The appointment or series of appointments was cancelled.",
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
      ApiResponse(
        responseCode = "404",
        description = "The appointment for this id was not found.",
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
  fun cancelAppointment(
    @PathVariable("appointmentId") appointmentId: Long,
    @Valid
    @RequestBody
    @Parameter(
      description = "The cancel request with the cancellation details and how to apply the cancellation",
      required = true,
    )
    request: AppointmentCancelRequest,
    principal: Principal,
  ) = appointmentService.cancelAppointment(appointmentId, request, principal)

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PostMapping(value = ["/{prisonCode}/search"])
  @Operation(
    summary = "Search for appointments within the specified prison",
    description =
    """
    Uses the supplied prison code and search parameters to filter and return appointment search results.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "Prison code and search parameters were accepted and results returned.",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = AppointmentSearchResult::class)),
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
  fun searchAppointments(
    @PathVariable("prisonCode") prisonCode: String,
    @Valid
    @RequestBody
    @Parameter(
      description = "The search parameters to use to filter appointments",
      required = true,
    )
    request: AppointmentSearchRequest,
    principal: Principal,
  ): List<AppointmentSearchResult> = appointmentSearchService.searchAppointments(prisonCode, request, principal)
}
