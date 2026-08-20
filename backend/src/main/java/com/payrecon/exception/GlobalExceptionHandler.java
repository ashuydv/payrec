package com.payrecon.exception;

import com.payrecon.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.of(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidStatusTransitionException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.of(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(MaxRetriesExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxRetriesExceeded(MaxRetriesExceededException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.of(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.ofFieldErrors(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        "Validation failed for one or more fields",
                        request.getRequestURI(),
                        fieldErrors
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                        "An unexpected error occurred", request.getRequestURI())
        );
    }
}
