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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySuitabilityCriteria
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.AttendanceReasonService
import java.time.LocalDate

@RestController
@RequestMapping("/integration-api", produces = [MediaType.APPLICATION_JSON_VALUE])
class IntegrationApiController(
  private val attendancesService: AttendancesService,
  private val scheduledInstanceService: ScheduledInstanceService,
  private val attendanceReasonService: AttendanceReasonService,
  private val activityService: ActivityService,
  private val activityScheduleService: ActivityScheduleService,
  private val waitingListService: WaitingListService,
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

  @Operation(
    summary = "Get the list of attendance reasons",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Attendance reasons found",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = AttendanceReason::class)))],
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
  @GetMapping("/attendance-reasons")
  @ResponseBody
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN', 'ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getAttendanceReasons(): List<AttendanceReason> = attendanceReasonService.getAll()

  @Operation(
    summary = "Get the capacity and number of allocated slots in an activity",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activity schedules",
        useReturnTypeSchema = true,
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ActivityScheduleLite::class)),
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
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Activity ID not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/activities/{activityId}/schedules"])
  @ResponseBody
  @PreAuthorize("hasAnyRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getActivitySchedules(@PathVariable("activityId") activityId: Long): List<ActivityScheduleLite> = activityService.getSchedulesForActivity(activityId)

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
            array = ArraySchema(schema = Schema(implementation = ScheduledActivity::class)),
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
  ): List<ScheduledActivity> = scheduledInstanceService.getActivityScheduleInstancesForPrisonerByDateRange(
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    startDate = startDate,
    endDate = endDate,
    slot = slot,
  )

  @GetMapping(value = ["/activities/schedule/{scheduleId}/suitability-criteria"])
  @ResponseBody
  @Operation(
    summary = "Gets the suitability criteria for allocating prisoners to a particular activity ",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ActivitySuitabilityCriteria::class)),
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The suitability criteria was not found.",
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
  fun getActivityScheduleSuitabilityCriteria(
    @PathVariable("scheduleId")
    @Parameter(description = "Schedule ID", required = true)
    scheduleId: Long,
  ): ActivitySuitabilityCriteria? = activityScheduleService.getSuitabilityCriteria(
    scheduleId = scheduleId,
  )

  @GetMapping(value = ["/schedules/{scheduleId}/waiting-list-applications"])
  @ResponseBody
  @Operation(
    summary = "Get a schedules waiting list applications",
    description = "Returns zero or more activity schedule waiting list applications.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The waiting list applications for an activity schedule",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = WaitingListApplication::class)),
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
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Schedule ID not found",
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
  @PreAuthorize("hasRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getWaitingListApplicationsBy(@PathVariable("scheduleId") scheduleId: Long) = waitingListService.getWaitingListsBySchedule(scheduleId)
}
