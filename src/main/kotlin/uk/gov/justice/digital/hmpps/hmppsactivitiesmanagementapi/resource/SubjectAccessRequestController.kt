package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SubjectAccessRequestContent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.SubjectAccessRequestService
import java.time.LocalDate

/**
 * Prisoners have the right to access and receive a copy of their personal data and other supplementary information.
 *
 * This is commonly referred to as a subject access request or ‘SAR’.
 */
@RestController
@Tag(name = "Subject Access Request")
@PreAuthorize(" hasRole('SAR_DATA_ACCESS')")
@RequestMapping("/subject-access-request", produces = [MediaType.APPLICATION_JSON_VALUE])
class SubjectAccessRequestController(private val service: SubjectAccessRequestService) {

  @GetMapping
  @Operation(
    summary = "Provides content for a prisoner to satisfy the needs of a subject access request on their behalf",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Request successfully processed - content found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = SubjectAccessRequestContent::class))],
      ),
      ApiResponse(
        responseCode = "204",
        description = "Request successfully processed - no content found",
      ),
      ApiResponse(
        responseCode = "209",
        description = "Subject Identifier is not recognised by this service",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "The client does not have authorisation to make this request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error occurred",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getSarContentByReference(
    @RequestParam(name = "prn", required = false)
    @Parameter(description = "NOMIS Prison Reference Number")
    prn: String?,
    @RequestParam(name = "crn", required = false)
    @Parameter(description = "nDelius Case Reference Number")
    crn: String?,
    @RequestParam(value = "fromDate", required = false)
    @Parameter(description = "Optional parameter denoting minimum date of event occurrence which should be returned in the response")
    fromDate: LocalDate?,
    @RequestParam(value = "toDate", required = false)
    @Parameter(description = "Optional parameter denoting maximum date of event occurrence which should be returned in the response")
    toDate: LocalDate?,
  ): ResponseEntity<Any> {
    if (crn != null) {
      return ResponseEntity.status(209).body(
        ErrorResponse(
          status = 209,
          userMessage = "Search by case reference number is not supported.",
          developerMessage = "Search by case reference number is not supported.",
        ),
      )
    }

    return prn
      ?.takeIf(String::isNotBlank)
      ?.let { service.getContentFor(prn, fromDate, toDate) }
      ?.let { content -> ResponseEntity.ok(content) }
      ?: ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body(
          ErrorResponse(
            status = HttpStatus.NO_CONTENT,
            userMessage = "No content found for the prisoner number.",
            developerMessage = "No content found for the prisoner number.",
          ),
        )
  }
}
