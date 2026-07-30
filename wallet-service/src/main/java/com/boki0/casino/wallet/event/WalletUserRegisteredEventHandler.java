package com.boki0.casino.wallet.event;

import com.boki0.casino.wallet.dto.CreateWalletRequest;
import com.boki0.casino.wallet.dto.WalletResponse;
import com.boki0.casino.wallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WalletUserRegisteredEventHandler implements UserRegisteredEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletUserRegisteredEventHandler.class);

    private final WalletService walletService;

    public WalletUserRegisteredEventHandler(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public void handle(UserRegisteredEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("UserRegisteredEvent must not be null");
        }

        LOGGER.info(
                "Handling UserRegisteredEvent eventId={}, authUserId={}",
                event.eventId(),
                event.authUserId()
        );

        WalletResponse walletResponse = walletService.createWalletForUser(
                new CreateWalletRequest(event.authUserId(), "EUR")
        );

        LOGGER.info(
                "UserRegisteredEvent handled successfully eventId={}, authUserId={}, walletId={}",
                event.eventId(),
                event.authUserId(),
                walletResponse.id()
        );
    }
}
