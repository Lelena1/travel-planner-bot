package io.github.lelena1.travelbot.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Кеш ответов внешних API в памяти (Caffeine).
 * Справочник городов почти не меняется, а цены Aviasales и так обновляются раз в несколько часов,
 * поэтому повторные запросы можно не отправлять: бот отвечает быстрее и не упирается в лимиты API.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

	public static final String PLACES = "places";
	public static final String FLIGHT_PRICES = "flight-prices";
	public static final String CITY_COORDINATES = "city-coordinates";
	public static final String SIGHTS = "sights";
	public static final String RESTAURANTS = "restaurants";

	@Bean
	CacheManager cacheManager() {
		CaffeineCacheManager manager = new CaffeineCacheManager();
		manager.registerCustomCache(PLACES, Caffeine.newBuilder()
				.expireAfterWrite(Duration.ofDays(7))
				.maximumSize(1_000)
				.build());
		manager.registerCustomCache(FLIGHT_PRICES, Caffeine.newBuilder()
				.expireAfterWrite(Duration.ofMinutes(30))
				.maximumSize(500)
				.build());
		// города не переезжают, достопримечательности и рестораны меняются редко,
		// а Википедия и OpenStreetMap отвечают небыстро
		manager.registerCustomCache(CITY_COORDINATES, Caffeine.newBuilder()
				.expireAfterWrite(Duration.ofDays(30))
				.maximumSize(1_000)
				.build());
		manager.registerCustomCache(SIGHTS, Caffeine.newBuilder()
				.expireAfterWrite(Duration.ofDays(7))
				.maximumSize(200)
				.build());
		manager.registerCustomCache(RESTAURANTS, Caffeine.newBuilder()
				.expireAfterWrite(Duration.ofDays(7))
				.maximumSize(200)
				.build());
		return manager;
	}
}
