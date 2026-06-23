package com.boki0.casino.user.event;

import com.boki0.casino.user.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserProfileUserRegisteredEventHandler implements UserRegisteredEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileUserRegisteredEventHandler.class);

    private final UserProfileService userProfileService;

    public UserProfileUserRegisteredEventHandler(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @Override
    public void handle(UserRegisteredEvent event) {
        LOGGER.info(
                "Handling UserRegisteredEvent eventId={}, authUserId={}",
                event.eventId(),
                event.authUserId()
        );
        userProfileService.createProfileFromUserRegisteredEvent(event);
        LOGGER.info(
                "UserRegisteredEvent handled successfully eventId={}, authUserId={}",
                event.eventId(),
                event.authUserId()
        );
    }
}
