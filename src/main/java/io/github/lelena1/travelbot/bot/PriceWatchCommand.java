package io.github.lelena1.travelbot.bot;

import java.util.Optional;

import org.springframework.stereotype.Component;

import io.github.lelena1.travelbot.pricewatch.PriceChecker;
import io.github.lelena1.travelbot.pricewatch.PriceWatchService;

/** Раздел «🔔 Мои подписки» и кнопки «Следить за ценой» / «Отписаться». */
@Component
public class PriceWatchCommand {

	private final PriceWatchService watches;
	private final PriceChecker checker;

	public PriceWatchCommand(PriceWatchService watches, PriceChecker checker) {
		this.watches = watches;
		this.checker = checker;
	}

	public BotReply list(long userId) {
		return watches.list(userId);
	}

	/** Нажатие кнопки «watch:12» или «unwatch:5»; пусто, если текст — не такая кнопка. */
	public Optional<BotReply> handleAction(long userId, long chatId, String text) {
		Optional<Long> tripId = idAfter(text, PriceWatchService.WATCH_ACTION);
		if (tripId.isPresent()) {
			return Optional.of(watches.subscribe(userId, chatId, tripId.get()));
		}
		Optional<Long> watchId = idAfter(text, PriceWatchService.UNWATCH_ACTION);
		return watchId.map(id -> watches.unsubscribe(userId, id));
	}

	/** /checkprices — проверить все подписки сейчас, не дожидаясь расписания. */
	public BotReply checkNow() {
		PriceChecker.Summary summary = checker.checkAll();
		return BotReply.text("""
				🔄 Проверка цен завершена
				Подписок проверено: %d
				Уведомлений отправлено: %d
				Завершено (поездка началась или удалена): %d
				Не удалось проверить: %d"""
				.formatted(summary.checked(), summary.notified(), summary.finished(), summary.failed()));
	}

	private static Optional<Long> idAfter(String text, String prefix) {
		if (!text.startsWith(prefix)) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.parseLong(text.substring(prefix.length())));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
