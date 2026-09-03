package org.poc.objs.service.web

import org.poc.objs.api.validation.ValidationIssue
import org.poc.objs.api.validation.ValidationResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Map request-binding failures onto the registry validation shape. */
@RestControllerAdvice(assignableTypes = [ObjsRegistryController::class])
class ObjsRegistryExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ValidationResult> {
        val detail = ex.mostSpecificCause.message ?: ex.message ?: "Malformed request body"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ValidationResult.of(
                ValidationIssue("SCHEMA_REQUEST_INVALID", detail),
            ),
        )
    }
}
