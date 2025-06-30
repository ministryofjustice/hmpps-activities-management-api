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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AdvanceAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AdvanceAttendanceCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AdvanceAttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AdvanceAttendanceService
import java.security.Principal

@RestController
@RequestMapping("/advance-attendances", produces = [MediaType.APPLICATION_JSON_VALUE])
class AdvanceAttendanceController(private val advanceAttendanceService: AdvanceAttendanceService) {

  @GetMapping(value = ["/{advanceAttendanceId}"])
  @ResponseBody
  @Operation(
    summary = "Get an advance attendance by id.",
    description = "Returns an advance attendance.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Advance Attendance found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AdvanceAttendance::class))],
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
        description = "The advance attendance was not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getAttendanceById(
    @PathVariable("advanceAttendanceId") instanceId: Long,
  ): AdvanceAttendance = advanceAttendanceService.getAttendanceById(instanceId)

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  @Operation(
    summary = "Create an advance attendance",
    description = "Create an advance attendance. Currently only for a prisoner who is not required.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The advance attendance record was created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AdvanceAttendance::class))],
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
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun create(
    principal: Principal,
    @Valid
    @Parameter(description = "The create request with the new advance attendance", required = true)
    @RequestBody request: AdvanceAttendanceCreateRequest,
  ): AdvanceAttendance = advanceAttendanceService.create(request, principal.name)

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PutMapping(value = ["/{advanceAttendanceId}"])
  @ResponseBody
  @Operation(
    summary = "Updates an advance attendance.",
    description = "Updates the given advance attendance. Currently only updates the issue payment status.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "The advance attendance was updated.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AdvanceAttendance::class))],
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
        description = "The advance attendance was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun update(
    principal: Principal,
    @PathVariable("advanceAttendanceId") advanceAttendanceId: Long,
    @RequestBody request: AdvanceAttendanceUpdateRequest,
  ) = advanceAttendanceService.update(advanceAttendanceId, request.issuePayment!!, principal.name)

  @DeleteMapping(value = ["/{advanceAttendanceId}"])
  @ResponseBody
  @Operation(
    summary = "Deletes the advance attendance.",
    description = "Hard deletes the given advance attendance.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The advance attendance was deleted.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AdvanceAttendance::class))],
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
        description = "The advance attendance was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun reset(
    @PathVariable("advanceAttendanceId") advanceAttendanceId: Long,
  ) = advanceAttendanceService.delete(advanceAttendanceId)
}
