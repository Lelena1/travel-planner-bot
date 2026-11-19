package io.github.lelena1.travelbot.bot.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.longpolling.util.ExponentialBackOff;

class TelegramBotConfigTest {

	@Test
	void reconnectPauseNeverExceedsHalfAMinuteAndNeverGivesUp() {
		ExponentialBackOff backOff = TelegramBotConfig.reconnectBackOff();

		long longest = 0;
		for (int attempt = 0; attempt < 100; attempt++) {   // ~ час без связи
			long pause = backOff.nextBackOffMillis();
			assertThat(pause).as("попытка %d", attempt).isPositive();
			longest = Math.max(longest, pause);
		}
		// с учётом случайного разброса, который библиотека добавляет к паузе
		assertThat(longest).isLessThanOrEqualTo((long) (TelegramBotConfig.MAX_RECONNECT_PAUSE_MILLIS * 1.5));
	}
}
