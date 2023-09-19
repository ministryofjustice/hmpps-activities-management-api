package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException

@RestControllerAdvice
class ControllerAdvice(private val mapper: ObjectMapper) : ResponseEntityExceptionHandler() {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.info("Access denied exception: {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = FORBIDDEN.reasonPhrase,
        ),
      )
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
        ),
      )
  }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = INTERNAL_SERVER_ERROR.reasonPhrase,
        ),
      )
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ErrorResponse> {
    log.info("Entity not found exception: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND.value(),
          userMessage = NOT_FOUND.reasonPhrase,
        ),
      )
  }

  @ExceptionHandler(CaseloadAccessException::class)
  fun handleCaseLoadAccessException(e: CaseloadAccessException): ResponseEntity<ErrorResponse> {
    log.info("Case load access exception: {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN.value(),
          userMessage = FORBIDDEN.reasonPhrase,
        ),
      )
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
    log.info("Error converting '${e.name}' (${e.value}). Method argument type mismatch exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST.value(),
          userMessage = BAD_REQUEST.reasonPhrase,
        ),
      )
  }

  @ExceptionHandler(WebClientResponseException::class)
  fun handleWebClientResponseException(ex: WebClientResponseException): ResponseEntity<ErrorResponse> {
    log.info("Web client response exception: {}", ex.message)
    var errorResponse: ErrorResponse? = null
    try {
      errorResponse = mapper.readValue(ex.responseBodyAsString, ErrorResponse::class.java)
      log.error("Web client response was : {}", errorResponse)
    } catch (jpe: JsonProcessingException) {
      log.error("Failed to parse web client response as ErrorResponse: {}", ex.message)
    } catch (jme: JsonProcessingException) {
      log.error("Failed to parse web client response as ErrorResponse: {}", ex.message)
    }
    return ResponseEntity
      .status(ex.statusCode)
      .body(
        ErrorResponse(
          status = ex.statusCode.value(),
          userMessage = ex.statusText,
        ),
      )
  }

  @ExceptionHandler(IllegalArgumentException::class)
  fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
    log.info("Exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Exception: ${e.message}",

        ),
      )
  }

  override fun handleHttpMessageNotReadable(
    ex: HttpMessageNotReadableException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any>? {
    log.info("Exception not readable: {}", ex.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = BAD_REQUEST.reasonPhrase,
        ),
      )
  }

  override fun handleMethodArgumentNotValid(
    ex: MethodArgumentNotValidException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any>? {
    val errors = ex.bindingResult.allErrors.map { it.defaultMessage }

    log.info("Constraint errors: {}", errors)

    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = BAD_REQUEST.reasonPhrase,
        ),
      )
  }

  override fun handleMissingServletRequestParameter(
    ex: MissingServletRequestParameterException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any>? {
    log.info("missing servlet errors: {}", ex)

    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = BAD_REQUEST.reasonPhrase,
        ),
      )
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, moreInfo)
}
