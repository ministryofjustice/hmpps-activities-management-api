package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.EventReviewSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.EventReviewSearchResults
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.EventReviewService
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/event-review", produces = [MediaType.APPLICATION_JSON_VALUE])
class EventReviewController(private val eventReviewService: EventReviewService) {

  @GetMapping(value = ["/prison/{prisonCode}"])
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get events for a prison which may indicate that a change of circumstances affecting allocations had occurred",
    description = "Returns events in the prison which match the search criteria provided.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Search performed successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = EventReview::class),
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
  fun getEventsForReview(
    @PathVariable("prisonCode")
    @Parameter(description = "The 3-letter prison code")
    prisonCode: String,

    @RequestParam(required = true)
    @Parameter(description = "The date for which to request events, in format YYYY-MM-DD, e.b. 2023-10-01")
    date: LocalDate,

    @RequestParam(required = false, defaultValue = "null")
    @Parameter(description = "The prisoner number, eg. A9999AA")
    prisonerNumber: String?,

    @RequestParam(required = false, defaultValue = "false")
    @Parameter(description = "Whether to include acknowledged events, un-acknowledged events or both. Values true, false or null.")
    acknowledged: Boolean?,

    @RequestParam(required = false, defaultValue = "0")
    @Parameter(description = "The page number, eg. 1")
    page: Int,

    @RequestParam(required = false, defaultValue = "10")
    @Parameter(description = "How many items to return in a page, e.g. 10")
    size: Int,

    @RequestParam(required = false, defaultValue = "")
    @Parameter(description = "The sort direction e.g ASC, DESC")
    sortDirection: String,
  ): EventReviewSearchResults {
    val filters = EventReviewSearchRequest(
      prisonCode = prisonCode,
      eventDate = date,
      prisonerNumber = prisonerNumber,
      acknowledgedEvents = acknowledged,
    )
    val paginatedResults = eventReviewService.getFilteredEvents(page, size, sortDirection, filters)
    return EventReviewSearchResults(paginatedResults.content, paginatedResults.number, paginatedResults.totalPages)
  }
}
