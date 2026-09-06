package com.example.demo.api;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> api(ApiException exception) {
        return ResponseEntity.status(exception.status)
            .body(ApiErrorResponse.of(exception.code, exception.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class})
    public ResponseEntity<ApiErrorResponse> invalidRequest(Exception exception) {
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of("invalid_request", "Request body or parameters are invalid"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> unknown(Exception exception, HttpServletRequest request) {
        Throwable rootCause = rootCause(exception);
        log.error("Unhandled API exception for {} {}: {}: {}", request.getMethod(), request.getRequestURI(),
            rootCause.getClass().getSimpleName(), rootCause.getMessage());
        log.debug("Full unhandled API exception for {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.internalServerError()
            .body(ApiErrorResponse.of("internal_error", "An unexpected error occurred"));
    }

    private Throwable rootCause(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
