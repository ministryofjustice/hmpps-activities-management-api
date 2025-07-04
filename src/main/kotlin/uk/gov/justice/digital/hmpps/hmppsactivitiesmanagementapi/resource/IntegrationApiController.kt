package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceService
import java.time.LocalDate

@RestController
@RequestMapping("/integration-api", produces = [MediaType.APPLICATION_JSON_VALUE])
class IntegrationApiController(
  private val attendancesService: AttendancesService,
  private val scheduledInstanceService: ScheduledInstanceService,
) {
  @GetMapping(value = ["/attendances/{prisonerNumber}"])
  @ResponseBody
  @Operation(
    summary = "Gets a list of prisoner attendance activities for a given date range",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = Attendance::class)),
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
        description = "The attendance was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getAttendanceForPrisoner(
    @PathVariable("prisonerNumber")
    @Parameter(description = "Prisoner Number")
    prisonerNumber: String,
    @RequestParam("startDate", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Start date of query (required). Format YYYY-MM-DD.")
    startDate: LocalDate,
    @RequestParam("endDate", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "End date of query (required). Format YYYY-MM-DD.")
    endDate: LocalDate,
    @RequestParam("prisonCode", required = false)
    @Parameter(description = "The 3-character prison code.")
    prisonCode: String? = null,
  ): List<Attendance> = attendancesService.getPrisonerAttendance(
    prisonerNumber = prisonerNumber,
    startDate = startDate,
    endDate = endDate,
    prisonCode = prisonCode,
  )

  @GetMapping(value = ["/prisons/{prisonCode}/{prisonerNumber}/scheduled-instances"])
  @ResponseBody
  @Operation(
    summary = "Get a list of scheduled instances for a prisoner, prison, date range (max 3 months) and time slot (AM, PM or ED - optional)",
    description = "Returns zero or more scheduled instances for a prisoner and date range (max 3 months).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - zero or more scheduled instance records found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ActivityScheduleInstance::class)),
          ),
        ],
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
    ],
  )
  @PreAuthorize("hasRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getScheduledInstancesForPrisoner(
    @PathVariable("prisonCode", required = true)
    @Parameter(description = "The 3-character prison code.")
    prisonCode: String,
    @PathVariable("prisonerNumber", required = true)
    @Parameter(description = "Prisoner Number")
    prisonerNumber: String,
    @RequestParam("startDate", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Start date of query (required). Format YYYY-MM-DD.")
    startDate: LocalDate,
    @RequestParam("endDate", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "End date of query (required). The end date must be within 3 months of the start date. Format YYYY-MM-DD.")
    endDate: LocalDate,
    @RequestParam(value = "slot")
    @Parameter(description = "The time slot (optional). If supplied, one of AM, PM or ED.")
    slot: TimeSlot?,
    @RequestParam(value = "cancelled")
    @Parameter(description = "Return cancelled scheduled instances?")
    cancelled: Boolean?,
  ): List<ActivityScheduleInstance> = scheduledInstanceService.getActivityScheduleInstancesForPrisonerByDateRange(
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    startDate = startDate,
    endDate = endDate,
    slot = slot,
    cancelled = cancelled,
  )
}
