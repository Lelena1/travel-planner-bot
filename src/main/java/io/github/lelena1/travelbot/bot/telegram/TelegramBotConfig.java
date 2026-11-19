package io.github.lelena1.travelbot.bot.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import io.github.lelena1.travelbot.bot.CommandRouter;
import io.github.lelena1.travelbot.config.BotProperties;

/**
 * Подключение к Telegram через long polling (бот сам опрашивает сервер Telegram,
 * поэтому не нужен публичный адрес и HTTPS-сертификат).
 * Включается, только если задан токен BOT_TOKEN.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("!'${bot.token:}'.isBlank()")
public class TelegramBotConfig {

	private static final Logger log = LoggerFactory.getLogger(TelegramBotConfig.class);

	@Bean
	TelegramClient telegramClient(BotProperties properties) {
		return new OkHttpTelegramClient(properties.token());
	}

	@Bean
	TelegramSender telegramSender(TelegramClient client) {
		return new TelegramSender(client);
	}

	@Bean
	TelegramUpdateConsumer telegramUpdateConsumer(TelegramClient client, TelegramSender sender, CommandRouter router) {
		return new TelegramUpdateConsumer(client, sender, router);
	}

	@Bean(destroyMethod = "close")
	TelegramBotsLongPollingApplication telegramBotsApplication(BotProperties properties,
			TelegramUpdateConsumer consumer) throws TelegramApiException {
		TelegramBotsLongPollingApplication application = new TelegramBotsLongPollingApplication();
		application.registerBot(properties.token(), consumer);
		log.info("Бот @{} подключён к Telegram", properties.username());
		return application;
	}
}
