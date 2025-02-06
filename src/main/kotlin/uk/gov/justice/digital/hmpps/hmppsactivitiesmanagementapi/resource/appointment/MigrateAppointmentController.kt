package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.appointment

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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.DeleteMigratedAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCountSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.MigrateAppointmentService
import java.time.LocalDate

@RestController
@RequestMapping("/migrate-appointment", produces = [MediaType.APPLICATION_JSON_VALUE])
class MigrateAppointmentController(
  private val migrateAppointmentService: MigrateAppointmentService,
  private val deleteMigratedAppointmentsJob: DeleteMigratedAppointmentsJob,
) {
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping()
  @Operation(
    summary = "Migrate an appointment from NOMIS",
    description =
    """
    Migrate an appointment creating an appointment series with one appointment that has the supplied prisoner allocated.
    Will return null if appointment was dropped.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The appointment was migrated.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentInstance::class),
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
  @PreAuthorize("hasRole('NOMIS_APPOINTMENTS')")
  fun migrateAppointment(
    @Valid
    @RequestBody
    @Parameter(
      description = "The migration request with the appointment details",
      required = true,
    )
    request: AppointmentMigrateRequest,
  ) = migrateAppointmentService.migrateAppointment(request)

  @ResponseStatus(HttpStatus.ACCEPTED)
  @DeleteMapping(value = ["/{prisonCode}"])
  @Operation(
    summary = """
      Starts a job to delete migrated appointments taking place at the supplied prison code that start on or after the
      supplied start date and are assigned the optional category code.
      """,
    description = """
      Migrated appointments matching the supplied criteria will be soft deleted in the database. An appointment instance
      deleted domain event used for syncing will be published for each deleted appointment. This will cause the mapped
      appointment in NOMIS to also be deleted.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "Migrated appointments matching the supplied criteria are being deleted.",
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
  @PreAuthorize("hasAnyRole('NOMIS_APPOINTMENTS', 'ACTIVITY_ADMIN', 'MIGRATE_APPOINTMENTS')")
  fun deleteMigratedAppointments(
    @PathVariable("prisonCode")
    @Parameter(description = "The 3-digit prison code.")
    prisonCode: String,
    @RequestParam(value = "startDate", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Inclusive start date of migrated appointments to be deleted. Must be today or in the future")
    startDate: LocalDate,
    @RequestParam(value = "categoryCode", required = false)
    @Parameter(description = "The category code assigned to migrated appointments to be deleted.")
    categoryCode: String? = null,
  ) {
    require(startDate >= LocalDate.now()) {
      "Start date must not be in the past"
    }
    deleteMigratedAppointmentsJob.execute(prisonCode, startDate, categoryCode)
  }

  @Operation(
    summary = "Get the list of appointment categories",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment summary details",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = AppointmentCountSummary::class)))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @GetMapping(value = ["/{prisonCode}/summary"])
  @ResponseBody
  @PreAuthorize("hasAnyRole('NOMIS_APPOINTMENTS', 'MIGRATE_APPOINTMENTS')")
  fun migratedAppointmentsSummary(
    @PathVariable("prisonCode")
    @Parameter(description = "The 3-digit prison code")
    prisonCode: String,
    @RequestParam(value = "startDate", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Inclusive start date of migrated appointments. Must be today or in the future")
    startDate: LocalDate,
    @RequestParam(value = "categoryCodes", required = true)
    @Parameter(description = "A list of category codes to retrieve summaries appointments")
    categoryCodes: List<String>,
  ): List<AppointmentCountSummary> {
    require(startDate >= LocalDate.now()) {
      "Start date must not be in the past"
    }
    return migrateAppointmentService.getAppointmentSummary(prisonCode, startDate, categoryCodes)
  }
}
