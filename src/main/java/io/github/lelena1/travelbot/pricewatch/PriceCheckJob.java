package io.github.lelena1.travelbot.pricewatch;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Проверка цен по расписанию: по умолчанию в 8:00, 14:00 и 20:00 по Москве —
 * достаточно часто, чтобы не пропустить снижение, и без уведомлений ночью.
 */
@Component
public class PriceCheckJob {

	private final PriceChecker checker;

	public PriceCheckJob(PriceChecker checker) {
		this.checker = checker;
	}

	@Scheduled(cron = "${price-watch.cron:0 0 8,14,20 * * *}", zone = "Europe/Moscow")
	public void run() {
		checker.checkAll();
	}
}
