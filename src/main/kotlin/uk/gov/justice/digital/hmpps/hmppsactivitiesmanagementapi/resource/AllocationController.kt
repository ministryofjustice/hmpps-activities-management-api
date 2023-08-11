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
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import java.security.Principal
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.DeallocationReason as ModelDeallocationReason

@RestController
@RequestMapping("/allocations", produces = [MediaType.APPLICATION_JSON_VALUE])
class AllocationController(
  private val allocationsService: AllocationsService,
  private val waitingListService: WaitingListService,
) {

  @GetMapping(value = ["/id/{allocationId}"])
  @ResponseBody
  @Operation(
    summary = "Get an allocation by its id",
    description = "Returns a single allocation and its details by its unique identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "allocation found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Allocation::class),
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
        description = "The allocation for this ID was not found.",
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
  fun getAllocationById(@PathVariable("allocationId") allocationId: Long) =
    allocationsService.getAllocationById(allocationId)

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
  @GetMapping(value = ["/deallocation-reasons"])
  @ResponseBody
  fun getDeallocationReasons() = DeallocationReason.toModelDeallocationReasons()

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PatchMapping(value = ["/{prisonCode}/allocationId/{allocationId}"])
  @Operation(
    summary = "Update an allocation",
    description = "Update an allocation. Requires any one of the following roles ['ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN'].",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "The allocation was updated.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Allocation::class),
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
        description = "Allocation ID not found",
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
  fun update(
    @PathVariable("allocationId") allocationId: Long,
    @PathVariable("prisonCode") prisonCode: String,
    principal: Principal,
    @Valid
    @RequestBody
    @Parameter(
      description = "The update request with the new allocation details",
      required = true,
    )
    allocation: AllocationUpdateRequest,
  ): Allocation = allocationsService.updateAllocation(allocationId, allocation, prisonCode, principal.name)

  @PostMapping(value = ["/{prisonCode}/waiting-list-application"])
  @Operation(
    summary = "Add a prisoner to an activity schedule waiting list",
    description = "Adds the supplied waiting list creation request to the activity schedule. Requires any one of the following roles ['ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN'].",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The waiting list entry was created and added to the schedule.",
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
        description = "The activity schedule in the request for this ID was not found.",
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
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  fun addToWaitingList(
    @PathVariable("prisonCode") prisonCode: String,
    principal: Principal,
    @Valid
    @RequestBody
    @Parameter(
      description = "The request with the prisoner waiting list details",
      required = true,
    )
    request: WaitingListApplicationRequest,
  ): ResponseEntity<Any> =
    waitingListService.addPrisoner(prisonCode, request, principal.name)
      .let { ResponseEntity.noContent().build() }
}
