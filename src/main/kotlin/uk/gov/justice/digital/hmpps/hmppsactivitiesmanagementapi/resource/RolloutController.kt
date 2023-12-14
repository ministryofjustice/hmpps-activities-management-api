package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.RolloutPrisonService

@RestController
@RequestMapping("/rollout", produces = [MediaType.APPLICATION_JSON_VALUE])
class RolloutController(
  private val rolloutService: RolloutPrisonService,
) {

  @GetMapping(value = ["/{prisonCode}"])
  @Operation(
    summary = "Get a prison's rollout plan by prison code",
    description = "Returns a single prison and its activities management service rollout plan by its unique code.",
  )
  @PreAuthorize("hasRole('ACTIVITY_ADMIN')")
  @ResponseBody
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Prison rollout plan found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = RolloutPrisonPlan::class),
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
  fun getPrisonByCode(@PathVariable("prisonCode") prisonCode: String): RolloutPrisonPlan =
    rolloutService.getByPrisonCode(prisonCode)

  @Operation(
    summary = "Get all rollout prisons",
    description = "Returns a list of all rolled out prisons.",
  )
  @GetMapping
  @PreAuthorize("hasRole('ACTIVITY_ADMIN')")
  @ResponseBody
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of prisons that are rolled out",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = RolloutPrisonPlan::class),
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
  fun getRolledOutPrisons(): List<RolloutPrisonPlan> =
    rolloutService.getRolloutPrisons()
}
