package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.appointment

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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCategoryRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentCategoryService

@RestController
@RequestMapping("/appointment-categories", produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentCategoryController(
  private val appointmentCategoryService: AppointmentCategoryService,
  private val appointmentCategoryRepository: AppointmentCategoryRepository,
) {

  @GetMapping
  @ResponseBody
  @Operation(
    summary = "Get the list of appointment categories",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment categories found",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = AppointmentCategorySummary::class)))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getAppointmentCategories(): List<AppointmentCategorySummary> = appointmentCategoryService.get()

  @GetMapping(value = ["/{categoryId}"])
  @ResponseBody
  @Operation(
    summary = "Get the appointment category by id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment category found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AppointmentCategory::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Appointment category not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getAppointmentCategoryById(
    @PathVariable("categoryId") categoryId: Long,
  ): AppointmentCategory = appointmentCategoryRepository.findOrThrowNotFound(categoryId).toModel()

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create an appointment category",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Appointment category created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AppointmentCategory::class))],
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
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun createAppointmentCategory(
    @Valid
    @Parameter(description = "The create request with the new appointment category", required = true)
    @RequestBody request: AppointmentCategoryRequest,
  ): AppointmentCategory = appointmentCategoryService.create(request)

  @PutMapping(value = ["/{categoryId}"])
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(
    summary = "Update an appointment category",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "Appointment category updated",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AppointmentCategory::class))],
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
        description = "Appointment category not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun updateAppointmentCategory(
    @PathVariable("categoryId") categoryId: Long,
    @Valid
    @Parameter(description = "The update request with the new appointment category details", required = true)
    @RequestBody request: AppointmentCategoryRequest,
  ): AppointmentCategory = appointmentCategoryService.update(categoryId, request)

  @DeleteMapping(value = ["/{categoryId}"])
  @ResponseBody
  @Operation(
    summary = "Delete an appointment category",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment category deleted",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AppointmentCategory::class))],
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
        description = "Appointment category not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun deleteAppointmentCategory(
    @PathVariable("categoryId") categoryId: Long,
  ) = appointmentCategoryService.delete(categoryId)
}
