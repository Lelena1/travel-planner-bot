package io.github.lelena1.travelbot.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/**
 * Общие настройки HTTP-клиентов для внешних API.
 * <p>
 * Бот обрабатывает сообщения по одному, поэтому зависший запрос остановил бы его целиком —
 * отсюда тайм-ауты. И User-Agent: открытые сервисы (Википедия, OpenStreetMap) просят
 * представляться, чтобы связаться с автором, если запросов будет слишком много.
 */
@Configuration(proxyBeanMethods = false)
public class HttpClientConfig {

	static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
	public static final String USER_AGENT =
			"TravelPlannerBot/1.0 (personal Telegram bot; https://github.com/Lelena1/travel-planner-bot)";

	@Bean
	RestClientCustomizer timeoutsAndUserAgent() {
		return builder -> {
			HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
			JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
			factory.setReadTimeout(READ_TIMEOUT);
			builder.requestFactory(factory).defaultHeader("User-Agent", USER_AGENT);
		};
	}
}
