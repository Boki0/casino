package com.boki0.casino.game.exception;

import com.boki0.casino.game.api.dto.ApiErrorResponse;
import com.boki0.casino.game.domain.InvalidGameSessionStateException;
import com.boki0.casino.game.provider.exception.GameSyncException;
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

    @ExceptionHandler(GameSessionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleGameSessionNotFoundException(
            GameSessionNotFoundException exception,
            HttpServletRequest request
    ) {
        return errorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(GameSessionAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleGameSessionAccessDeniedException(
            GameSessionAccessDeniedException exception,
            HttpServletRequest request
    ) {
        return errorResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler({GameUnavailableException.class, InvalidGameSessionStateException.class})
    public ResponseEntity<ApiErrorResponse> handleGameConflictException(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return errorResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return errorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
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

    @ExceptionHandler(GameSyncException.class)
    public ResponseEntity<ApiErrorResponse> handleGameSyncException(
            GameSyncException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = switch (exception.getErrorType()) {
            case INVALID_GAME_DATA -> HttpStatus.UNPROCESSABLE_ENTITY;
            case MISSING_PROVIDER -> HttpStatus.CONFLICT;
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

    private ResponseEntity<ApiErrorResponse> errorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }
}
