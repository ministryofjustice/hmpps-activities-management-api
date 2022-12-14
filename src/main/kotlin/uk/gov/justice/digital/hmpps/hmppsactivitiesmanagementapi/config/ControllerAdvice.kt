package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.lang.Nullable
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.util.WebUtils
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

@RestControllerAdvice
class ControllerAdvice(private val mapper: ObjectMapper) : ResponseEntityExceptionHandler() {
  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Access denied exception: {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "Access denied: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message
        )
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
          userMessage = "Not found: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
    log.info("Method argument type mismatch exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST.value(),
          userMessage = "Error converting '${e.name}' (${e.value}): ${e.message.orEmpty().substringBefore("; nested exception is")
          }",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(WebClientResponseException::class)
  fun handleWebClientResponseException(ex: WebClientResponseException): ResponseEntity<ErrorResponse> {
    log.info("Web client response exception: {}", ex.message)
    var errorResponse: ErrorResponse? = null
    try {
      errorResponse = mapper.readValue(ex.responseBodyAsString, ErrorResponse::class.java)
    } catch (jpe: JsonProcessingException) {
      log.error("Failed to parse web client response as ErrorResponse: {}", ex.message)
    } catch (jme: JsonProcessingException) {
      log.error("Failed to parse web client response as ErrorResponse: {}", ex.message)
    }
    return ResponseEntity
      .status(HttpStatus.valueOf(ex.rawStatusCode))
      .body(
        errorResponse ?: ErrorResponse(
          status = ex.rawStatusCode,
          userMessage = ex.message,
          developerMessage = ex.message
        )
      )
  }

  @ExceptionHandler(IllegalArgumentException::class)
  fun handleIllegalArgumentException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Exception: ${e.message}",
          developerMessage = e.message
        )
      )
  }

  override fun handleMethodArgumentNotValid(
    ex: MethodArgumentNotValidException,
    headers: HttpHeaders,
    status: HttpStatus,
    request: WebRequest
  ): ResponseEntity<Any> {
    val errors = ex.bindingResult.allErrors.map { it.defaultMessage }

    log.info("Constraint errors: {}", errors)

    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "One or more constraint violations occurred",
          developerMessage = errors.joinToString(", ")
        )
      )
  }

  override fun handleExceptionInternal(
    ex: java.lang.Exception,
    @Nullable body: Any?,
    headers: HttpHeaders,
    status: HttpStatus,
    request: WebRequest
  ): ResponseEntity<Any> {
    if (INTERNAL_SERVER_ERROR == status) {
      request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST)
    } else {
      if (body == null) {
        return ResponseEntity(
          ErrorResponse(
            status = status,
            userMessage = ex.message,
            developerMessage = ex.message
          ),
          headers,
          status
        )
      }
    }
    return ResponseEntity(body, headers, status)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
