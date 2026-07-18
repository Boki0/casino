package com.boki0.casino.game.exception;

import com.boki0.casino.game.api.dto.ApiErrorResponse;
import com.boki0.casino.game.provider.exception.ProviderCatalogException;
import com.boki0.casino.game.provider.exception.ProviderSyncException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GameExceptionHandler {

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleGameNotFoundException(
            GameNotFoundException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ProviderCatalogException.class)
    public ResponseEntity<ApiErrorResponse> handleProviderCatalogException(
            ProviderCatalogException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ProviderSyncException.class)
    public ResponseEntity<ApiErrorResponse> handleProviderSyncException(
            ProviderSyncException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = switch (exception.getErrorType()) {
            case INVALID_PROVIDER_DATA -> HttpStatus.UNPROCESSABLE_ENTITY;
            case PERSISTENCE_FAILURE -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }
}
