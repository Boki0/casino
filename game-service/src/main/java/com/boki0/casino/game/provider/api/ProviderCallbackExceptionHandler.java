package com.boki0.casino.game.provider.api;

import com.boki0.casino.game.provider.dto.ProviderAuthenticateResponse;
import com.boki0.casino.game.provider.dto.ProviderBetResponse;
import com.boki0.casino.game.provider.exception.ProviderAuthenticationException;
import com.boki0.casino.game.wallet.exception.WalletClientException;
import jakarta.servlet.http.HttpServletRequest;
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

    private static final String BET_PATH = "/api/provider-wallet/bet";
    private static final int INVALID_REQUEST_ERROR = 1000;
    private static final int WALLET_ERROR = 2001;
    private static final int INSUFFICIENT_BALANCE_ERROR = 2002;
    private static final int WALLET_NOT_FOUND_ERROR = 2003;
    private static final int IDEMPOTENCY_CONFLICT_ERROR = 2004;

    @ExceptionHandler(ProviderAuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationFailure(
            ProviderAuthenticationException exception,
            HttpServletRequest request
    ) {
        if (isBetRequest(request)) {
            return ResponseEntity.ok(ProviderBetResponse.error(
                    exception.getErrorCode(),
                    exception.getPublicDescription()
            ));
        }
        return ResponseEntity.status(exception.getStatus()).body(
                ProviderAuthenticateResponse.error(
                        exception.getErrorCode(),
                        exception.getPublicDescription()
                )
        );
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<?> handleInvalidRequest(HttpServletRequest request) {
        if (isBetRequest(request)) {
            return ResponseEntity.ok(
                    ProviderBetResponse.error(INVALID_REQUEST_ERROR, "Invalid Bet request")
            );
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ProviderAuthenticateResponse.error(
                        INVALID_REQUEST_ERROR,
                        "Invalid authentication request"
                )
        );
    }

    @ExceptionHandler(WalletClientException.class)
    public ResponseEntity<ProviderBetResponse> handleWalletFailure(
            WalletClientException exception
    ) {
        return ResponseEntity.ok(switch (exception.getCategory()) {
            case INSUFFICIENT_BALANCE -> ProviderBetResponse.error(
                    INSUFFICIENT_BALANCE_ERROR,
                    "Insufficient wallet balance"
            );
            case WALLET_NOT_FOUND -> ProviderBetResponse.error(
                    WALLET_NOT_FOUND_ERROR,
                    "Wallet not found"
            );
            case IDEMPOTENCY_CONFLICT -> ProviderBetResponse.error(
                    IDEMPOTENCY_CONFLICT_ERROR,
                    "Bet reference conflict"
            );
            default -> ProviderBetResponse.error(WALLET_ERROR, "Bet could not be processed");
        });
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleUnexpectedFailure(HttpServletRequest request) {
        if (isBetRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ProviderBetResponse.error(WALLET_ERROR, "Bet could not be processed")
            );
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ProviderAuthenticateResponse.error(
                        WALLET_ERROR,
                        "Authentication could not be processed"
                )
        );
    }

    private boolean isBetRequest(HttpServletRequest request) {
        return BET_PATH.equals(request.getRequestURI());
    }
}
