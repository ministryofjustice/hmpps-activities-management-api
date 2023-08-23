package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import java.security.Principal

@RestController
@Validated
@RequestMapping("/waiting-list-applications", produces = [MediaType.APPLICATION_JSON_VALUE])
class WaitingListApplicationController(private val waitingListService: WaitingListService) {
  @GetMapping(value = ["/{waitingListId}"])
  @ResponseBody
  @Operation(
    summary = "Get a waiting list application by its id",
    description = "Returns a single waiting list application by its unique identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Waiting list application found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = WaitingListApplication::class),
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
        description = "The waiting list application for this ID was not found.",
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
  fun getWaitingListById(@PathVariable("waitingListId") id: Long) = waitingListService.getWaitingListBy(id)

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PatchMapping(value = ["/{waitingListId}"])
  @Operation(
    description = "Update a waiting list application.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "The updated waiting list application.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = WaitingListApplication::class),
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
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_ADMIN')")
  fun updateWaitingList(
    @PathVariable("waitingListId") waitingListId: Long,
    principal: Principal,
    @Valid
    @RequestBody
    @Parameter(
      description = "The update request with the waiting list changes",
      required = true,
    )
    request: WaitingListApplicationUpdateRequest,
  ) = waitingListService.updateWaitingList(waitingListId, request, principal.name)
}
