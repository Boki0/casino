package com.boki0.casino.game.provider.api;

import com.boki0.casino.game.provider.auth.ProviderSessionAuthenticationService;
import com.boki0.casino.game.provider.bet.ProviderBetService;
import com.boki0.casino.game.provider.dto.ProviderAuthenticateRequest;
import com.boki0.casino.game.provider.dto.ProviderAuthenticateResponse;
import com.boki0.casino.game.provider.dto.ProviderBetRequest;
import com.boki0.casino.game.provider.dto.ProviderBetResponse;
import com.boki0.casino.game.provider.dto.ProviderResultRequest;
import com.boki0.casino.game.provider.dto.ProviderResultResponse;
import com.boki0.casino.game.provider.result.ProviderResultService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provider-wallet")
public class ProviderWalletController {

    private final ProviderSessionAuthenticationService authenticationService;
    private final ProviderBetService betService;
    private final ProviderResultService resultService;

    public ProviderWalletController(
            ProviderSessionAuthenticationService authenticationService,
            ProviderBetService betService,
            ProviderResultService resultService
    ) {
        this.authenticationService = authenticationService;
        this.betService = betService;
        this.resultService = resultService;
    }

    @PostMapping("/authenticate")
    public ProviderAuthenticateResponse authenticate(
            @Valid @RequestBody ProviderAuthenticateRequest request
    ) {
        return authenticationService.authenticate(request);
    }

    @PostMapping(
            path = "/bet",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ProviderBetResponse bet(@Valid @RequestBody ProviderBetRequest request) {
        return betService.process(request);
    }

    @PostMapping(
            path = "/result",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ProviderResultResponse result(@Valid @RequestBody ProviderResultRequest request) {
        return resultService.process(request);
    }
}
