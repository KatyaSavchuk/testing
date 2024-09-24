package io.proj3ct.Spring.config;

import lombok.Data;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.beans.factory.annotation.Value;
@Configuration
@PropertySource("application.properties")
@Data
public class BotConfig {
    @Value("${bot.name}")
    String botName;

    @Value("${bot.token}")
    String token;

}
