package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.EventReviewSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.EventReviewSearchResults
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.EventReviewService
import java.time.LocalDate

@RestController
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
    @PathVariable("prisonCode", required = true)
    @Parameter(description = "The prison code e.g. MDI")
    @NotEmpty(message = "Prison code must be supplied")
    prisonCode: String,

    @RequestParam(required = true)
    @Parameter(description = "The date for which to request events, format YYYY-MM-DD, e.g. 2023-10-01")
    @PastOrPresent(message = "The date supplied must be today or a date in the past.")
    date: LocalDate,

    @RequestParam(required = false)
    @Parameter(description = "The prisoner number, eg. A9999AA (optional). Default is all prisoner numbers.")
    prisonerNumber: String?,

    @RequestParam(required = false, defaultValue = "false")
    @Parameter(description = "Whether to include acknowledged events (optional). Default is false.")
    includeAcknowledged: Boolean? = false,

    @RequestParam(required = false, defaultValue = "0")
    @Parameter(description = "The page number to return (optional). Default is page zero.")
    @PositiveOrZero(message = "Page number cannot be negative.")
    page: Int = 0,

    @RequestParam(required = false, defaultValue = "10")
    @Parameter(description = "The maximum number of items to return in each page (optional). Default is 10.")
    @Positive(message = "Page size must be a positive number.")
    size: Int = 10,

    @RequestParam(required = false, defaultValue = "ascending")
    @Parameter(description = "The sort direction based on the time the events occurred. Default is ascending.")
    sortDirection: String = "ascending",
  ): EventReviewSearchResults {
    val filters = EventReviewSearchRequest(
      prisonCode = prisonCode,
      eventDate = date,
      prisonerNumber = prisonerNumber,
      acknowledgedEvents = includeAcknowledged,
    )
    val paginatedResults = eventReviewService.getFilteredEvents(page, size, sortDirection, filters)
    return EventReviewSearchResults(paginatedResults.content, paginatedResults.number, paginatedResults.totalPages)
  }
}
