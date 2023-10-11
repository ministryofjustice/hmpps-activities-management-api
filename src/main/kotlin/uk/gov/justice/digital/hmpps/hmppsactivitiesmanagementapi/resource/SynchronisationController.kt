package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllocationReconciliationResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceSync
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.SynchronisationService

@RestController
@RequestMapping("/synchronisation", produces = [MediaType.APPLICATION_JSON_VALUE])
class SynchronisationController(private val synchronisationService: SynchronisationService) {

  @PreAuthorize("hasRole('NOMIS_ACTIVITIES')")
  @GetMapping("/attendance/{attendanceId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Retrieves Nomis synchronisation details",
    description = "Retrieves all details required in order to synchronise an attendance with the Nomis database.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Attendance retrieved",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = AttendanceSync::class)),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "There was an error with the request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Attendance not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getAttendanceSync(
    @Schema(description = "Attendance id", required = true) @PathVariable attendanceId: Long,
  ) =
    synchronisationService.findAttendanceSync(attendanceId)
      ?: throw EntityNotFoundException("Attendance sync not found: $attendanceId")

  @PreAuthorize("hasRole('NOMIS_ACTIVITIES')")
  @GetMapping("/reconciliation/allocations/{prisonId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Retrieves allocation details for the sync reconciliation",
    description = "Retrieves booking numbers and counts for allocations currently active in each prison",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Reconciliation information retrieved",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = AllocationReconciliationResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "There was an error with the request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getAllocationReconciliation(
    @Schema(description = "Prison id", required = true) @PathVariable prisonId: String,
  ): AllocationReconciliationResponse = synchronisationService.findActiveAllocationsSummary(prisonId)
}
