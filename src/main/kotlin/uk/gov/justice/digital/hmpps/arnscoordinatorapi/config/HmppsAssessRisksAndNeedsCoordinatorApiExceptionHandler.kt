package uk.gov.justice.digital.hmpps.arnscoordinatorapi.config

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestControllerAdvice
class HmppsAssessRisksAndNeedsCoordinatorApiExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  // MethodArgumentNotValidException is thrown when one or more body parameters are invalid.
  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleArgumentNotValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
    val parsedErrors = e.bindingResult.fieldErrors
      .map { "${it.codes?.get(0)?.substringAfter("#")?.substringAfter(".")} - ${it.defaultMessage}" }

    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: $parsedErrors",
          developerMessage = "$parsedErrors",
        ),
      ).also {
        log.info("MethodArgumentNotValidException: {}", "$parsedErrors")
      }
  }

  // HandlerMethodValidationException if thrown when a path parameter is invalid.
  // If both the path parameter and body are invalid, this will also contain the validation errors for the body.
  @ExceptionHandler(HandlerMethodValidationException::class)
  fun handlerMethodNotValidationException(e: HandlerMethodValidationException): ResponseEntity<ErrorResponse> {
    val parsedErrors = e.allValidationResults
      .map { it.resolvableErrors }
      .flatMap { it }
      .map { "${it.codes?.get(0)?.substringAfter("#")?.substringAfter(".")} - ${it.defaultMessage}" }

    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: $parsedErrors",
          developerMessage = "$parsedErrors",
        ),
      ).also {
        log.info("HandlerMethodValidationException: {}", parsedErrors)
      }
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleNoRequestException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(FORBIDDEN)
    .body(
      ErrorResponse(
        status = FORBIDDEN,
        userMessage = "Forbidden: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.debug("Forbidden (403) returned: {}", e.message) }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.error("Unexpected exception", e) }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
