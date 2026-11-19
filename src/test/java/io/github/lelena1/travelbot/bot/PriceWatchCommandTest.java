package io.github.lelena1.travelbot.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.pricewatch.PriceChecker;
import io.github.lelena1.travelbot.pricewatch.PriceWatchService;

class PriceWatchCommandTest {

	private PriceWatchService watches;
	private PriceChecker checker;
	private PriceWatchCommand command;

	@BeforeEach
	void setUp() {
		watches = mock(PriceWatchService.class);
		checker = mock(PriceChecker.class);
		command = new PriceWatchCommand(watches, checker);
	}

	@Test
	void watchButtonSubscribes() {
		BotReply subscribed = BotReply.text("🔔 Слежу");
		when(watches.subscribe(1, 100, 12)).thenReturn(subscribed);

		assertThat(command.handleAction(1, 100, "watch:12")).contains(subscribed);
	}

	@Test
	void unwatchButtonUnsubscribes() {
		BotReply unsubscribed = BotReply.text("🔕");
		when(watches.unsubscribe(1, 5)).thenReturn(unsubscribed);

		assertThat(command.handleAction(1, 100, "unwatch:5")).contains(unsubscribed);
	}

	@Test
	void otherTextIsNotAnAction() {
		assertThat(command.handleAction(1, 100, "Рим")).isEmpty();
		assertThat(command.handleAction(1, 100, "watch:abc")).isEmpty();
		verifyNoInteractions(watches);
	}

	@Test
	void checkNowReportsSummary() {
		when(checker.checkAll()).thenReturn(new PriceChecker.Summary(3, 1, 1, 0));

		assertThat(command.checkNow().text())
				.contains("Подписок проверено: 3")
				.contains("Уведомлений отправлено: 1");
	}
}
