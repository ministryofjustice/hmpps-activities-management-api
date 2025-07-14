package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import jakarta.validation.ValidationException
import org.springframework.data.domain.Page
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.weeksAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySuitabilityCriteria
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivitySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledEventService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentSearchService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.AttendanceReasonService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import java.security.Principal
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.DeallocationReason as ModelDeallocationReason

@RestController
@RequestMapping("/integration-api", produces = [MediaType.APPLICATION_JSON_VALUE])
class IntegrationApiController(
  private val attendancesService: AttendancesService,
  private val scheduledInstanceService: ScheduledInstanceService,
  private val scheduledEventService: ScheduledEventService,
  private val referenceCodeService: ReferenceCodeService,
  private val attendanceReasonService: AttendanceReasonService,
  private val activityService: ActivityService,
  private val activityScheduleService: ActivityScheduleService,
  private val waitingListService: WaitingListService,
  private val prisonRegimeService: PrisonRegimeService,
  private val appointmentSearchService: AppointmentSearchService
) {
  @Operation(
    summary = "Get the list of deallocation reasons",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Deallocation reasons found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ModelDeallocationReason::class)),
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
  @GetMapping(value = ["/allocations/deallocation-reasons"])
  @ResponseBody
  @PreAuthorize("hasAnyRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getDeallocationReasons() = DeallocationReason.toModelDeallocationReasons()

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
  @PreAuthorize("hasAnyRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
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

  @GetMapping(value = ["/schedules/{scheduleId}"])
  @ResponseBody
  @Operation(
    summary = "Get an activity schedule by its id",
    description = "Returns a single activity schedule by its unique identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activity found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ActivitySchedule::class),
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
        description = "The activity for this ID was not found.",
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
  @PreAuthorize("hasAnyRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getScheduleById(
    @PathVariable("scheduleId") scheduleId: Long,
    @RequestParam(value = "earliestSessionDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "If provided will filter earliest sessions >= the given date. Format YYYY-MM-DD, otherwise defaults to 4 weeks prior to the current date.")
    earliestSessionDate: LocalDate?,
  ) = activityScheduleService.getScheduleById(scheduleId, earliestSessionDate ?: 4.weeksAgo())

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

  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = ["/waiting-list-applications/{prisonCode}/search"])
  @Operation(
    description = "Search waiting list applications",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Waiting list application search results",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PagedWaitingListApplication::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
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
        description = "Waiting list application not found",
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
  @PreAuthorize("hasAnyRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun searchWaitingLists(
    @PathVariable("prisonCode") prisonCode: String,
    @Valid
    @RequestBody
    @Parameter(
      description = "Search filters",
      required = true,
    )
    request: WaitingListSearchRequest,
    @Valid
    @Parameter(
      description = "The page number of search results (default: 0)",
      required = false,
    )
    @RequestParam("page")
    pageNumber: Int?,
    @Valid
    @Parameter(
      description = "The number of search results per page (default: 50)",
      required = false,
    )
    @RequestParam("pageSize")
    pageSize: Int?,
  ) = waitingListService.searchWaitingLists(prisonCode, request, pageNumber ?: 0, pageSize ?: 50)

  abstract inner class PagedWaitingListApplication : Page<WaitingListApplication>

  @Operation(
    summary = "Get list of activities running at a specified prison. " +
      "Optionally and by default, only currently LIVE activities are returned",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activities",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ActivitySummary::class)),
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
    ],
  )
  @GetMapping(value = ["/prison/{prisonCode}/activities"])
  @ResponseBody
  @PreAuthorize("hasAnyRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getActivities(
    @PathVariable("prisonCode") prisonCode: String,
    @RequestParam(value = "excludeArchived", required = false, defaultValue = "true") excludeArchived: Boolean,
    @RequestParam(value = "nameSearch", required = false)
    @Parameter(description = "The activity name contains this case insensitive search term")
    nameSearch: String?,
  ): List<ActivitySummary> = activityService.getActivitiesInPrison(prisonCode, excludeArchived, nameSearch = nameSearch)

  @GetMapping(value = ["/prison/{prisonCode}/prison-pay-bands"])
  @ResponseBody
  @Operation(
    summary = "Get a list of pay bands at a given prison",
    description = "Returns the pay bands at a given prison or a default list of values if none present.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Prison pay bands found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = PrisonPayBand::class)),
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
    ],
  )
  @PreAuthorize("hasAnyRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getPrisonPayBands(
    @PathVariable("prisonCode")
    prisonCode: String,
  ): List<PrisonPayBand> = prisonRegimeService.getPayBandsForPrison(prisonCode)

  @GetMapping(value = ["/prison/prison-regime/{prisonCode}"])
  @ResponseBody
  @Operation(
    summary = "Get a prison regime by its code",
    description = "Returns a single prison regime and its details by its unique prison code.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Prison regime found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = PrisonRegime::class)),
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
    ],
  )
  @PreAuthorize("hasAnyRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getPrisonRegimeByPrisonCode(@PathVariable("prisonCode") prisonCode: String): List<PrisonRegime> = prisonRegimeService.getPrisonRegimeByPrisonCode(prisonCode)

  @GetMapping(
    value = ["/scheduled-events/prison/{prisonCode}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = "Get a list of scheduled events for a prison, prisoner, date range (max 3 months) and optional time slot.",
    description = """
      Returns scheduled events for the prison, prisoner, date range (max 3 months) and optional time slot.
      Court hearings, adjudication hearings, transfers and visits come from NOMIS (via prison API).
      Activities and appointments come from either NOMIS or the local database depending on whether the prison is
      marked as active for appointments and/or activities.
      (Intended usage: Prisoner calendar / schedule)
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - zero or more scheduled events found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerScheduledEvents::class),
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
  @PreAuthorize("hasAnyRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getScheduledEventsForSinglePrisoner(
    @PathVariable("prisonCode")
    @Parameter(description = "The 3-digit prison code.")
    prisonCode: String,
    @RequestParam(value = "prisonerNumber", required = true)
    @Parameter(description = "Prisoner number (required). Format A9999AA.")
    prisonerNumber: String,
    @RequestParam(value = "startDate", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Start date of query (required). Format YYYY-MM-DD.")
    startDate: LocalDate,
    @RequestParam(value = "endDate", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "End date of query (required). Format YYYY-MM-DD. The end date must be within 3 months of the start date)")
    endDate: LocalDate,
    @RequestParam(value = "timeSlot", required = false)
    @Parameter(description = "Time slot for the events (optional). If supplied, one of AM, PM or ED.")
    timeSlot: TimeSlot?,
  ): PrisonerScheduledEvents? {
    val dateRange = LocalDateRange(startDate, endDate)
    if (endDate.isAfter(startDate.plusMonths(3))) {
      throw ValidationException("Date range cannot exceed 3 months")
    }
    return scheduledEventService.getScheduledEventsForSinglePrisoner(
      prisonCode,
      prisonerNumber,
      dateRange,
      timeSlot,
      referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
    )
  }

  @PostMapping(
    value = ["/scheduled-events/prison/{prisonCode}"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = "Get a list of scheduled events for a prison and list of prisoner numbers for a date and time slot",
    description = """
      Returns scheduled events for the prison, prisoner numbers, single date and an optional time slot.
      Court hearings, adjudication hearings, transfers and visits come from NOMIS (via prison API).
      Activities and appointments come from either NOMIS or the local database depending on whether the prison is
      marked as rolled-out for activities and/or appointments.
      (Intended usage: Unlock list)
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - zero or more scheduled events found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerScheduledEvents::class),
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
  @PreAuthorize("hasAnyRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getScheduledEventsForMultiplePrisoners(
    @PathVariable("prisonCode")
    @Parameter(description = "The 3-character prison code.")
    prisonCode: String,
    @RequestParam(value = "date", required = true)
    @Parameter(description = "The exact date to return events for (required) in format YYYY-MM-DD")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    date: LocalDate,
    @RequestParam(value = "timeSlot", required = false)
    @Parameter(description = "Time slot of the events (optional). If supplied, one of AM, PM or ED.")
    timeSlot: TimeSlot?,
    @RequestBody(required = true)
    @Parameter(description = "Set of prisoner numbers (required). Example ['G11234YI', 'B5234YI'].", required = true)
    prisonerNumbers: Set<String>,
  ): PrisonerScheduledEvents? = scheduledEventService.getScheduledEventsForMultiplePrisoners(
    prisonCode,
    prisonerNumbers,
    date,
    timeSlot,
    referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
  )

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PostMapping(value = ["/appointments/{prisonCode}/search"])
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
  @PreAuthorize("hasAnyRole('ACTIVITIES__HMPPS_INTEGRATION_API')")
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