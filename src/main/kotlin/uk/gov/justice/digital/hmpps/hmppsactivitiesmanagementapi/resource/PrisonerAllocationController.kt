package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.PrisonerAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationsService

@RestController
@RequestMapping("/prisons", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerAllocationController(private val allocationsService: AllocationsService) {
  @PostMapping(value = ["/{prisonCode}/prisoner-allocations"])
  @ResponseBody
  @Operation(
    summary = "Get all allocations for prisoners",
    description = "Returns zero or more allocations for the supplied list of prisoners.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The allocations for the prisoners",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = PrisonerAllocations::class))
          )
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      )
    ]
  )
  @ResponseStatus(HttpStatus.OK)
  fun prisonerAllocations(
    @PathVariable prisonCode: String,
    @RequestBody @Parameter(
      description = "The required prisoner numbers (mandatory)",
      required = true
    ) prisonerNumbers: List<String>,
    @RequestParam(
      value = "activeOnly",
      required = false
    ) @Parameter(description = "If true will only return active allocations. Defaults to true.") activeOnly: Boolean?,
  ) = allocationsService.findByPrisonCodeAndPrisonerNumbers(prisonCode, prisonerNumbers.toSet(), activeOnly ?: true)
}
