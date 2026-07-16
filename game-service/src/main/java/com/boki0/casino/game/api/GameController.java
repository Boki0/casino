package com.boki0.casino.game.api;

import com.boki0.casino.game.api.dto.GameResponse;
import com.boki0.casino.game.service.GameCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/games")
public class GameController {

    private final GameCatalogService gameCatalogService;

    public GameController(GameCatalogService gameCatalogService) {
        this.gameCatalogService = gameCatalogService;
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
}
