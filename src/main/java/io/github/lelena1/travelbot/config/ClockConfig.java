package io.github.lelena1.travelbot.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Часы как отдельный бин: в тестах их можно подменить на фиксированную дату,
 * чтобы тесты не зависели от того, какой сегодня день.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.system(ZoneId.of("Europe/Moscow"));
	}
}
