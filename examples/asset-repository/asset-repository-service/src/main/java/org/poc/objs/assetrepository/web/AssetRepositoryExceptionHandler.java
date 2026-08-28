package org.poc.objs.assetrepository.web;

import java.util.NoSuchElementException;
import org.poc.objs.assetrepository.service.ObjectWriteService;
import org.poc.objs.assetrepository.web.dto.ApiDtos;
import org.poc.objs.core.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AssetRepositoryExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<ApiDtos.ErrorBody> notFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiDtos.ErrorBody("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiDtos.ErrorBody> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiDtos.ErrorBody("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(ObjectWriteService.ConflictException.class)
    ResponseEntity<ApiDtos.ErrorBody> conflict(ObjectWriteService.ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiDtos.ErrorBody("CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ApiDtos.ErrorBody> validation(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiDtos.ErrorBody("VALIDATION", ex.getMessage()));
    }
}
