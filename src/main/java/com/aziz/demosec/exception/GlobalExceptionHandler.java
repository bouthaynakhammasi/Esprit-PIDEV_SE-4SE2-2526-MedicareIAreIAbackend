package com.aziz.demosec.exception;

import com.aziz.demosec.exception.ContentViolationException;
import com.aziz.demosec.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ContentViolationException.class)
    public ResponseEntity<?> handleContentViolation(ContentViolationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "error", "CONTENT_VIOLATION",
                "message", ex.getMessage(),
                "cleanedText", ex.getCleanedText()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "FORBIDDEN",
                "message", ex.getMessage() != null ? ex.getMessage() : "You are not allowed to perform this action"
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Not Found",
                "message", ex.getMessage()
        ));
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", ex.getMessage()
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleBadRequest(HttpMessageNotReadableException ex) {
        String details = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid request body",
                "details", details
        ));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {

        System.out.println("=== VALIDATION ERROR ===");

        Map<String, String> errors = new HashMap<>();

        // Field errors
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            System.out.println("Field: " + error.getField() + " → " + error.getDefaultMessage());
            errors.put(error.getField(), error.getDefaultMessage());
        });

        // Global errors (class-level constraints)
        ex.getBindingResult().getGlobalErrors().forEach(error -> {
            System.out.println("Global: " + error.getDefaultMessage());
            errors.put(error.getObjectName(), error.getDefaultMessage());
        });

        System.out.println("========================");

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex) {
        return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal Server Error",
                "message", ex.getMessage() != null ? ex.getMessage() : "No message available",
                "type", ex.getClass().getName()
        ));
    }
}
