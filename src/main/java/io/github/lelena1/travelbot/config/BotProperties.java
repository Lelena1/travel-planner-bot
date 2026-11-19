package io.github.lelena1.travelbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки подключения к Telegram.
 *
 * @param token    токен от @BotFather; если пустой, бот не подключается к Telegram
 *                 (удобно для тестов и локальной разработки)
 * @param username имя бота без @, нужно, чтобы распознавать команды вида /start@my_bot
 */
@ConfigurationProperties(prefix = "bot")
public record BotProperties(String token, String username) {
}
