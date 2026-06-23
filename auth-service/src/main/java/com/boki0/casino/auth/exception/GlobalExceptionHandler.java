package com.boki0.casino.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = new ErrorResponse(
                status.value(),
                "Validation Failed",
                getValidationMessage(exception),
                request.getRequestURI(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "Invalid request body",
                request.getRequestURI(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse response = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "Unexpected error occurred",
                request.getRequestURI(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(status).body(response);
    }

    private String getValidationMessage(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();

        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            return fieldError.getDefaultMessage();
        }

        return "Validation failed";
    }
}
