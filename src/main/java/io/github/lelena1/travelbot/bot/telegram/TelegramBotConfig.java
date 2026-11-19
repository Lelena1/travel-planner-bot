package io.github.lelena1.travelbot.bot.telegram;

import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.ExponentialBackOff;
import org.telegram.telegrambots.longpolling.util.TelegramOkHttpClientFactory;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import com.fasterxml.jackson.databind.ObjectMapper;

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

	/** Сообщения до 4 чатов обрабатываются одновременно — для личного бота с запасом. */
	@Bean(destroyMethod = "close")
	ChatDispatcher chatDispatcher() {
		return new ChatDispatcher(4);
	}

	@Bean
	TelegramUpdateConsumer telegramUpdateConsumer(TelegramClient client, TelegramSender sender, CommandRouter router,
			ChatDispatcher dispatcher) {
		return new TelegramUpdateConsumer(client, sender, router, dispatcher);
	}

	@Bean(destroyMethod = "close")
	TelegramBotsLongPollingApplication telegramBotsApplication(BotProperties properties,
			TelegramUpdateConsumer consumer) throws TelegramApiException {
		TelegramBotsLongPollingApplication application = new TelegramBotsLongPollingApplication(
				ObjectMapper::new,
				new TelegramOkHttpClientFactory.DefaultOkHttpClientCreator(),
				Executors::newSingleThreadScheduledExecutor,
				TelegramBotConfig::reconnectBackOff);
		application.registerBot(properties.token(), consumer);
		log.info("Бот @{} подключён к Telegram", properties.username());
		return application;
	}

	/**
	 * Пауза между попытками переподключиться к Telegram после ошибки связи.
	 * По умолчанию библиотека увеличивает её до 15 минут: после сна компьютера бот
	 * молчал четверть часа. Ограничиваем паузу 30 секундами и не сдаёмся никогда.
	 */
	static ExponentialBackOff reconnectBackOff() {
		return new ExponentialBackOff.Builder()
				.setInitialIntervalMillis(500)
				.setMultiplier(2)
				.setMaxIntervalMillis(MAX_RECONNECT_PAUSE_MILLIS)
				.setMaxElapsedTimeMillis(Integer.MAX_VALUE)
				.build();
	}

	static final int MAX_RECONNECT_PAUSE_MILLIS = 30_000;
}
