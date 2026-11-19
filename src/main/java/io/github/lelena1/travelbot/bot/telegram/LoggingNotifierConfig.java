package io.github.lelena1.travelbot.bot.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.lelena1.travelbot.bot.Notifier;

/**
 * Без токена бот не подключён к Telegram, поэтому уведомления только пишутся в журнал.
 * Так приложение и тесты работают и без Telegram.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("'${bot.token:}'.isBlank()")
public class LoggingNotifierConfig {

	private static final Logger log = LoggerFactory.getLogger(LoggingNotifierConfig.class);

	@Bean
	Notifier loggingNotifier() {
		return (chatId, message) -> {
			log.info("Уведомление для чата {} (Telegram не подключён): {}", chatId, message.text());
			return false;
		};
	}
}
