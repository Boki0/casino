package com.boki0.casino.game;

import com.boki0.casino.game.config.GameSessionProperties;
import com.boki0.casino.game.config.WalletServiceProperties;
import com.boki0.casino.game.provider.config.ProviderProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        ProviderProperties.class,
        GameSessionProperties.class,
        WalletServiceProperties.class
})
public class GameServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameServiceApplication.class, args);
    }
}
