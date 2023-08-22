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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentOccurrenceSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentOccurrenceSearchService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentOccurrenceService
import java.security.Principal

@RestController
@RequestMapping("/appointment-occurrences", produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentOccurrenceController(
  private val appointmentOccurrenceService: AppointmentOccurrenceService,
  private val appointmentOccurrenceSearchService: AppointmentOccurrenceSearchService,
) {
  @ResponseStatus(HttpStatus.ACCEPTED)
  @PatchMapping(value = ["/{appointmentOccurrenceId}"])
  @Operation(
    summary = "Update an appointment occurrence or series of appointment occurrences",
    description =
    """
    Update an appointment occurrence or series of appointment occurrences based on the applyTo property.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "The appointment occurrence or series of appointment occurrences was updated.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Appointment::class),
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
  fun updateAppointmentOccurrence(
    @PathVariable("appointmentOccurrenceId") appointmentOccurrenceId: Long,
    @Valid
    @RequestBody
    @Parameter(
      description = "The update request with the new appointment occurrence details and how to apply the update",
      required = true,
    )
    request: AppointmentOccurrenceUpdateRequest,
    principal: Principal,
  ): Appointment = appointmentOccurrenceService.updateAppointmentOccurrence(appointmentOccurrenceId, request, principal)

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PutMapping(value = ["/{appointmentOccurrenceId}/cancel"])
  @Operation(
    summary = "Cancel an appointment occurrence or series of appointment occurrences",
    description =
    """
    Cancel an appointment occurrence or series of appointment occurrences based on the applyTo property.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "The appointment occurrence or series of appointment occurrences was cancelled.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Appointment::class),
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
  fun cancelAppointmentOccurrence(
    @PathVariable("appointmentOccurrenceId") appointmentOccurrenceId: Long,
    @Valid
    @RequestBody
    @Parameter(
      description = "The cancel request with the appointment occurrence details and how to apply the cancellation",
      required = true,
    )
    request: AppointmentOccurrenceCancelRequest,
    principal: Principal,
  ) = appointmentOccurrenceService.cancelAppointmentOccurrence(appointmentOccurrenceId, request, principal)

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PostMapping(value = ["/{prisonCode}/search"])
  @Operation(
    summary = "Search for appointment occurrences within the specified prison",
    description =
    """
    Uses the supplied prison code and search parameters to filter and return appointment occurrence search results.
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
            array = ArraySchema(schema = Schema(implementation = AppointmentOccurrenceSearchResult::class)),
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
  fun searchAppointmentOccurrences(
    @PathVariable("prisonCode") prisonCode: String,
    @Valid
    @RequestBody
    @Parameter(
      description = "The search parameters to use to filter appointment occurrences",
      required = true,
    )
    request: AppointmentOccurrenceSearchRequest,
    principal: Principal,
  ): List<AppointmentOccurrenceSearchResult> = appointmentOccurrenceSearchService.searchAppointmentOccurrences(prisonCode, request, principal)
}
