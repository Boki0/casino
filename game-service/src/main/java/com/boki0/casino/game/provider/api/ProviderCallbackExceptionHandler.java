package com.boki0.casino.game.provider.api;

import com.boki0.casino.game.provider.dto.ProviderAuthenticateResponse;
import com.boki0.casino.game.provider.exception.ProviderAuthenticationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ProviderWalletController.class)
public class ProviderCallbackExceptionHandler {

    @ExceptionHandler(ProviderAuthenticationException.class)
    public ResponseEntity<ProviderAuthenticateResponse> handleAuthenticationFailure(
            ProviderAuthenticationException exception
    ) {
        return ResponseEntity.status(exception.getStatus()).body(
                ProviderAuthenticateResponse.error(
                        exception.getErrorCode(),
                        exception.getPublicDescription()
                )
        );
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ProviderAuthenticateResponse> handleInvalidRequest() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ProviderAuthenticateResponse.error(1000, "Invalid authentication request")
        );
    }
}
