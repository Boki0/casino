package com.boki0.casino.game.provider.api;

import com.boki0.casino.game.provider.auth.ProviderSessionAuthenticationService;
import com.boki0.casino.game.provider.dto.ProviderAuthenticateRequest;
import com.boki0.casino.game.provider.dto.ProviderAuthenticateResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provider-wallet")
public class ProviderWalletController {

    private final ProviderSessionAuthenticationService authenticationService;

    public ProviderWalletController(ProviderSessionAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/authenticate")
    public ProviderAuthenticateResponse authenticate(
            @Valid @RequestBody ProviderAuthenticateRequest request
    ) {
        return authenticationService.authenticate(request);
    }
}
