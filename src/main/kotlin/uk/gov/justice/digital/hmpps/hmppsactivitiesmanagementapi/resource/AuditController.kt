package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AuditRecordSearchFilters
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.LocalAuditSearchResults
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService

@RestController
@Validated
@RequestMapping("/audit", produces = [MediaType.APPLICATION_JSON_VALUE])
class AuditController(
  private val auditService: AuditService,
) {

  @PostMapping(value = ["/search"])
  @ResponseBody
  @Operation(
    summary = "Search for audit records",
    description = "Returns all records that match the search criteria.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Search performed successfully",
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
  fun getAuditRecords(
    @RequestParam(required = false, defaultValue = "0") page: Int,
    @RequestParam(required = false, defaultValue = "3") size: Int,
    @RequestParam(required = false, defaultValue = "") sortDirection: String,
    @RequestBody filters: AuditRecordSearchFilters,
  ): LocalAuditSearchResults {
    val paginatedResults = auditService.searchEvents(page, size, sortDirection, filters)
    return LocalAuditSearchResults(paginatedResults.content, paginatedResults.number, paginatedResults.totalPages)
  }
}
