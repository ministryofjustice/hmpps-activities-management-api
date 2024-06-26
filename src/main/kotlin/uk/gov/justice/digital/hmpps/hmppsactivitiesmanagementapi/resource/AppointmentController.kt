package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventTierType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUncancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentAttendeeByStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentAttendanceService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentSearchService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendanceStatus
import java.security.Principal
import java.time.LocalDate

@RestController
@RequestMapping("/appointments", produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentController(
  private val appointmentService: AppointmentService,
  private val appointmentAttendanceService: AppointmentAttendanceService,
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

  @GetMapping(
    value = ["{prisonCode}/attendance-summaries"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary =
    """
    Get a list of appointments scheduled to take place on the specified date along with the summary of their attendance.
    """,
    description =
    """
    Returns appointments scheduled to take place on the specified date along with the summary of their attendance.
    Will contain summary information about the appointments taking place on the date as well as counts of attendees,
    counts of those marked attended and non attended and the count of attendees with no attendance marked.
    This endpoint supports management level views of appointment attendance and statistics.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - zero or more scheduled appointments found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentAttendanceSummary::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Requested resource not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @CaseloadHeader
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getAppointmentAttendanceSummaries(
    @PathVariable("prisonCode")
    @Parameter(description = "The 3-digit prison code (required)")
    prisonCode: String,
    @RequestParam(value = "date", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Date of appointments (required). Format YYYY-MM-DD")
    date: LocalDate,
    @Parameter(description = "appointment category code")
    @RequestParam(value = "categoryCode", required = false) categoryCode: String? = null,
    @Parameter(description = "appointment custom name")
    @RequestParam(value = "customName", required = false) customName: String? = null,
  ) = appointmentAttendanceService.getAppointmentAttendanceSummaries(
    prisonCode = prisonCode,
    date = date,
    categoryCode = categoryCode,
    customName = customName,
  )

  @GetMapping(
    value = ["{prisonCode}/{status}/attendance"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary =
    """
    Get a list of appointments scheduled to take place on the specified date by status
    """,
    description =
    """
    Returns appointments scheduled to take place on the specified date by status
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - zero or more scheduled appointments found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentAttendeeByStatus::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Requested resource not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @CaseloadHeader
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getAppointmentAttendanceByStatus(
    @PathVariable("prisonCode")
    @Parameter(description = "The 3-character prison code") prisonCode: String,
    @PathVariable("status")
    @Parameter(description = "attendance status") status: AttendanceStatus,
    @RequestParam(value = "date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Date of appointments. Format YYYY-MM-DD") date: LocalDate,
    @Parameter(description = "appointment category code")
    @RequestParam(value = "categoryCode", required = false) categoryCode: String? = null,
    @Parameter(description = "appointment custom name")
    @RequestParam(value = "customName", required = false) customName: String? = null,
    @RequestParam(value = "prisonerNumber", required = false) prisonerNumber: String? = null,
    @RequestParam(value = "eventTier", required = false) eventTier: EventTierType? = null,
    @RequestParam(value = "organiserCode", required = false) organiserCode: String? = null,
  ): List<AppointmentAttendeeByStatus> = appointmentAttendanceService.getAppointmentAttendanceByStatus(
    prisonCode = prisonCode,
    status = status,
    date = date,
    prisonerNumber = prisonerNumber,
    categoryCode = categoryCode,
    customName = customName,
    eventTier = eventTier,
    organiserCode = organiserCode,
  )

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PutMapping(value = ["/{appointmentId}/attendance"])
  @Operation(
    summary = "Mark the attendance for an appointment",
    description =
    """
    Mark or update the attendance records for the attendees of an appointment. This sets the current attendance record
    for each supplied prison number, replacing any existing record. This supports both the initial recording of attendance
    and changing that attendance record. There are no restrictions on when attendance can be recorded. It can be done
    for past and future appointments.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "Attendance for the appointment was recorded.",
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
  fun markAttendance(
    @PathVariable("appointmentId") appointmentId: Long,
    @Valid
    @RequestBody
    @Parameter(
      description = "The lists of prison numbers to mark as attended and non-attended",
      required = true,
    )
    request: AppointmentAttendanceRequest,
    principal: Principal,
  ): Appointment = appointmentAttendanceService.markAttendance(appointmentId, request, principal)

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
  @PutMapping(value = ["/{appointmentId}/uncancel"])
  @Operation(
    summary = "Uncancel an appointment or series of appointments",
    description =
    """
    Uncancel an appointment or series of appointments based on the applyTo property.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "The appointment or series of appointments was uncancelled.",
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
  fun uncancelAppointment(
    @PathVariable("appointmentId") appointmentId: Long,
    @Valid
    @RequestBody
    @Parameter(
      description = "The uncancel request with the uncancellation details and how to apply the uncancellation",
      required = true,
    )
    request: AppointmentUncancelRequest,
    principal: Principal,
  ) = appointmentService.uncancelAppointment(appointmentId, request, principal)

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
