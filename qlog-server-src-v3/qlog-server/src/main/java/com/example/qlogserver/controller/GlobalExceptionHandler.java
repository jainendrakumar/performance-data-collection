package com.example.qlogserver.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 * Handles validation errors and other unexpected exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation exceptions (@Valid annotation failures).
     *
     * @param ex      The MethodArgumentNotValidException exception.
     * @param request The current web request.
     * @return ResponseEntity with BAD_REQUEST status and error details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation error for request {}: {}", request.getDescription(false), errors);
        return new ResponseEntity<>(Map.of("message", "Validation failed", "details", errors), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles generic exceptions.
     *
     * @param ex      The Exception.
     * @param request The current web request.
     * @return ResponseEntity with INTERNAL_SERVER_ERROR status.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unhandled exception for request {}: {}", request.getDescription(false), ex.getMessage(), ex);
        return new ResponseEntity<>(Map.of("message", "An internal server error occurred"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Add more specific exception handlers as needed
    // e.g., @ExceptionHandler(AuthenticationException.class), @ExceptionHandler(DataAccessException.class)

}

