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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ClientDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCandidate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AllocationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CandidatesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.SecurityUtils
import java.security.Principal
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.ClientDetailsExtractor

// TODO add pre-auth annotations to enforce roles when we have them

@RestController
@RequestMapping("/schedules", produces = [MediaType.APPLICATION_JSON_VALUE])
class ActivityScheduleController(
  private val scheduleService: ActivityScheduleService,
  private val candidatesService: CandidatesService,
  private val clientDetailsExtractor: ClientDetailsExtractor
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
  fun getAllocationsBy(
    @PathVariable("scheduleId") scheduleId: Long,
    @RequestParam(
      value = "activeOnly",
      required = false,
    )
    @Parameter(description = "If true will only return active allocations. Defaults to true.")
    activeOnly: Boolean?,
    @RequestHeader(CASELOAD_ID) caseLoadId: String?,
    @Parameter(hidden = true) @RequestHeader(AUTHORIZATION) authToken: String,
  ): List<Allocation> {
    val client = clientDetailsExtractor.extract(caseLoadId = caseLoadId, bearerToken = authToken)
    return scheduleService.getAllocationsBy(scheduleId, activeOnly ?: true, client)
  }

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
  fun getScheduleId(
    @PathVariable("scheduleId") scheduleId: Long,
    @RequestHeader(CASELOAD_ID) caseLoadId: String?,
    @Parameter(hidden = true) @RequestHeader(AUTHORIZATION) authToken: String,
  ): ActivitySchedule {
    val client = clientDetailsExtractor.extract(caseLoadId = caseLoadId, bearerToken = authToken)
    return scheduleService.getScheduleById(scheduleId, client)
  }

  @PostMapping(value = ["/{scheduleId}/allocations"])
  @Operation(
    summary = "Allocate offender to schedule",
    description = "Allocates the supplied offender allocation request to the activity schedule. Requires any one of the following roles ['ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN'].",
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
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
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
  ): ResponseEntity<Any> =
    scheduleService.allocatePrisoner(scheduleId, prisonerAllocationRequest, principal.name)
      .let { ResponseEntity.noContent().build() }

  @GetMapping(value = ["/{scheduleId}/candidates"])
  @Operation(
    summary = "Get the suitable candidates for an activity",
    description = "Returns a paginated view of the list of candidates suitable for a given activity schedule." +
      " Filterable by employment status, workplace risk assessment, and incentive level." +
      " Requires any one of the following roles ['ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN'].",
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
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  fun candidates(
    @PathVariable("scheduleId") scheduleId: Long,
    @RequestParam(
      value = "suitableIncentiveLevel",
      required = false,
    ) suitableIncentiveLevel: List<String>?,
    @RequestParam(value = "suitableRiskLevel", required = false) suitableRiskLevel: List<String>?,
    @RequestParam(value = "suitableForEmployed", required = false) suitableForEmployed: Boolean?,
    @RequestParam(value = "search", required = false) search: String?,
    @ParameterObject @PageableDefault
    pageable: Pageable,
    @RequestHeader(CASELOAD_ID) caseLoadId: String?,
    @Parameter(hidden = true) @RequestHeader(AUTHORIZATION) authToken: String,
  ): Page<ActivityCandidate> {
    val client = clientDetailsExtractor.extract(caseLoadId = caseLoadId, bearerToken = authToken)
    val candidates = candidatesService.getActivityCandidates(
      scheduleId,
      suitableIncentiveLevel,
      suitableRiskLevel,
      suitableForEmployed,
      search,
      client,
    )

    val start = pageable.offset.toInt()
    val end = (start + pageable.pageSize).coerceAtMost(candidates.size)

    return PageImpl(
      candidates.subList(start.coerceAtMost(end), end),
      pageable,
      candidates.size.toLong(),
    )
  }

  @GetMapping(value = ["/{scheduleId}/suitability"])
  @Operation(
    summary = "Gets the suitability details of a candidate for an activity",
    description = "Returns candidate suitability details considering factors such as, workplace risk assessment," +
      " incentive level, education levels, earliest release date and non-associations" +
      " Requires any one of the following roles ['ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN'].",
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
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
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
    description = "Deallocates offenders from an activity schedule on a future date. Requires any one of the following roles ['ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN'].",
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
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
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
}
