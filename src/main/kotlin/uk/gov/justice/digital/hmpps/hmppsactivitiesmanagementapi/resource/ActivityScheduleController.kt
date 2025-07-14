package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.weeksAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCandidate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AllocationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CandidatesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import java.security.Principal
import java.time.LocalDate

@RestController
@RequestMapping("/schedules", produces = [MediaType.APPLICATION_JSON_VALUE])
class ActivityScheduleController(
  private val scheduleService: ActivityScheduleService,
  private val candidatesService: CandidatesService,
  private val waitingListService: WaitingListService,
) {

  @GetMapping(value = ["/{scheduleId}/allocations"])
  @ResponseBody
  @Operation(
    summary = "Get a list of activity schedule allocations",
    description = "Returns zero or more activity schedule allocations.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The allocations for an activity schedule",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = Allocation::class)),
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
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getAllocationsBy(
    @PathVariable("scheduleId") scheduleId: Long,
    @RequestParam(value = "activeOnly", required = false)
    @Parameter(description = "If true will only return active allocations. Defaults to true.")
    activeOnly: Boolean?,
    @RequestParam(value = "includePrisonerSummary", required = false)
    @Parameter(description = "If true will fetch and add prisoner details from prisoner search. Defaults to false.")
    includePrisonerSummary: Boolean?,
    @RequestParam(value = "date", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "If provided will filter allocations by the given date. Format YYYY-MM-DD.")
    date: LocalDate?,
  ) = scheduleService.getAllocationsBy(scheduleId, activeOnly ?: true, includePrisonerSummary ?: false, date)

  @GetMapping(value = ["/{scheduleId}"])
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
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN', 'NOMIS_ACTIVITIES', 'ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getScheduleById(
    @PathVariable("scheduleId") scheduleId: Long,
    @RequestParam(value = "earliestSessionDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "If provided will filter earliest sessions >= the given date. Format YYYY-MM-DD, otherwise defaults to 4 weeks prior to the current date.")
    earliestSessionDate: LocalDate?,
  ) = scheduleService.getScheduleById(scheduleId, earliestSessionDate ?: 4.weeksAgo())

  @PostMapping(value = ["/{scheduleId}/allocations"])
  @Operation(
    summary = "Allocate offender to schedule",
    description = "Allocates the supplied offender allocation request to the activity schedule.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The allocation was created and added to the schedule.",
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
        description = "The activity schedule for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_ADMIN')")
  fun allocate(
    principal: Principal,
    @PathVariable("scheduleId") scheduleId: Long,
    @RequestBody
    @Parameter(
      description = "The prisoner allocation request details",
      required = true,
    )
    @Valid
    prisonerAllocationRequest: PrisonerAllocationRequest,
  ): ResponseEntity<Any> = scheduleService.allocatePrisoner(scheduleId, prisonerAllocationRequest, principal.name)
    .let { ResponseEntity.noContent().build() }

  @GetMapping(value = ["/{scheduleId}/candidates"])
  @Operation(
    summary = "Get the suitable candidates for an activity",
    description = "Returns a paginated view of the list of candidates suitable for a given activity schedule." +
      " Filterable by employment status, workplace risk assessment, and incentive level.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A paginated list of candidates was returned.",
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
        description = "The activity schedule for this ID was not found.",
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
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_ADMIN')")
  fun candidates(
    @PathVariable("scheduleId") scheduleId: Long,
    @RequestParam(
      value = "suitableIncentiveLevel",
      required = false,
    ) suitableIncentiveLevels: List<String>?,
    @RequestParam(value = "suitableRiskLevel", required = false) suitableRiskLevels: List<String>?,
    @RequestParam(value = "suitableForEmployed", required = false) suitableForEmployed: Boolean?,
    @RequestParam(value = "noAllocations", required = false) noAllocations: Boolean?,
    @RequestParam(value = "search", required = false) search: String?,
    @ParameterObject @PageableDefault
    pageable: Pageable,
  ): Page<ActivityCandidate> = candidatesService.getActivityCandidates(
    scheduleId = scheduleId,
    suitableIncentiveLevels = suitableIncentiveLevels,
    suitableRiskLevels = suitableRiskLevels,
    suitableForEmployed = suitableForEmployed,
    noAllocations = noAllocations,
    search = search,
    pageable = pageable,
  )

  @GetMapping(value = ["/{scheduleId}/suitability"])
  @Operation(
    summary = "Gets the suitability details of a candidate for an activity",
    description = "Returns candidate suitability details considering factors such as, workplace risk assessment," +
      " incentive level, education levels, earliest release date and non-associations",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Candidate suitability details.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AllocationSuitability::class),
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
        description = "The activity schedule for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_ADMIN')")
  fun allocationSuitability(
    @PathVariable("scheduleId") scheduleId: Long,
    @RequestParam(value = "prisonerNumber", required = true)
    @Parameter(description = "Prisoner number (required). Format A9999AA.")
    prisonerNumber: String,
  ) = candidatesService.candidateSuitability(scheduleId, prisonerNumber)

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PutMapping(value = ["/{scheduleId}/deallocate"])
  @Operation(
    summary = "Deallocate offenders",
    description = "Deallocates offenders from an activity schedule on a future date.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "One or more prisoners were deallocated from the schedule.",
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
        description = "The activity schedule for this ID was not found.",
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
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_ADMIN')")
  fun deallocate(
    principal: Principal,
    @PathVariable("scheduleId") scheduleId: Long,
    @RequestBody
    @Parameter(
      description = "The prisoner deallocation request details",
      required = true,
    )
    @Valid
    deallocationRequest: PrisonerDeallocationRequest,
  ) {
    scheduleService.deallocatePrisoners(scheduleId, deallocationRequest, principal.name)
  }

  @GetMapping(value = ["/{scheduleId}/waiting-list-applications"])
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
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN', 'ACTIVITIES__HMPPS_INTEGRATION_API')")
  fun getWaitingListApplicationsBy(
    @PathVariable("scheduleId") scheduleId: Long,
    @RequestParam(value = "includeNonAssociationsCheck", required = false)
    @Parameter(description = "If true will try fetch and add non-association details. Defaults to true.")
    includeNonAssociationsCheck: Boolean?,
  ) = waitingListService.getWaitingListsBySchedule(scheduleId, includeNonAssociationsCheck ?: true)

  @GetMapping(value = ["/{scheduleId}/non-associations"])
  @ResponseBody
  @Operation(
    summary = "Get non-associations for a prisoner within an activity schedule",
    description = "Returns a list of non-associations for the prisoner.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Get non-associations for the prisoner",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = NonAssociationDetails::class)),
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
  @CaseloadHeader
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getNonAssociations(
    @PathVariable("scheduleId") scheduleId: Long,
    @RequestParam(value = "prisonerNumber", required = true)
    @Parameter(description = "Prisoner number. Format A9999AA.", required = true)
    prisonerNumber: String,
  ) = candidatesService.nonAssociations(scheduleId, prisonerNumber)
}
