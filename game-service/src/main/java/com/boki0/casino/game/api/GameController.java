package com.boki0.casino.game.api;

import com.boki0.casino.game.api.dto.GameLaunchRequest;
import com.boki0.casino.game.api.dto.GameLaunchResponse;
import com.boki0.casino.game.api.dto.GameResponse;
import com.boki0.casino.game.service.GameCatalogService;
import com.boki0.casino.game.service.GameLaunchResult;
import com.boki0.casino.game.service.GameLaunchService;
import com.boki0.casino.game.service.GameSessionLifecycleService;
import com.boki0.casino.game.service.PrepareGameLaunchCommand;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/games")
public class GameController {

    private static final String HEADER_AUTH_USER_ID = "X-Auth-User-Id";

    private final GameCatalogService gameCatalogService;
    private final GameLaunchService gameLaunchService;
    private final GameSessionLifecycleService gameSessionLifecycleService;

    public GameController(
            GameCatalogService gameCatalogService,
            GameLaunchService gameLaunchService,
            GameSessionLifecycleService gameSessionLifecycleService
    ) {
        this.gameCatalogService = gameCatalogService;
        this.gameLaunchService = gameLaunchService;
        this.gameSessionLifecycleService = gameSessionLifecycleService;
    }

    @GetMapping
    public List<GameResponse> getEnabledGames() {
        return gameCatalogService.getEnabledGames();
    }

    @GetMapping("/{id}")
    public GameResponse getEnabledGameById(@PathVariable UUID id) {
        return gameCatalogService.getEnabledGameById(id);
    }

    @GetMapping("/slug/{slug}")
    public GameResponse getEnabledGameBySlug(@PathVariable String slug) {
        return gameCatalogService.getEnabledGameBySlug(slug);
    }

    @PostMapping("/{gameId}/launch")
    public GameLaunchResponse launchGame(
            @PathVariable UUID gameId,
            @RequestHeader(HEADER_AUTH_USER_ID) UUID playerId,
            @Valid @RequestBody GameLaunchRequest request
    ) {
        GameLaunchResult result = gameLaunchService.launch(
                new PrepareGameLaunchCommand(gameId, playerId, request.currency())
        );
        return new GameLaunchResponse(result.localSessionId(), result.launchUrl());
    }

    @PostMapping("/sessions/{sessionId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeGameSession(
            @PathVariable UUID sessionId,
            @RequestHeader(HEADER_AUTH_USER_ID) UUID playerId
    ) {
        gameSessionLifecycleService.close(sessionId, playerId);
    }
}
