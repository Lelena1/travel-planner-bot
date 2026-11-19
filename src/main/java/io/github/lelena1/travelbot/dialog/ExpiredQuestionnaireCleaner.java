package io.github.lelena1.travelbot.dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.github.lelena1.travelbot.util.RussianPlural;

/** Раз в час удаляет брошенные анкеты вместе с черновиками поездок. */
@Component
public class ExpiredQuestionnaireCleaner {

	private static final Logger log = LoggerFactory.getLogger(ExpiredQuestionnaireCleaner.class);

	private final NewTripDialog dialog;

	public ExpiredQuestionnaireCleaner(NewTripDialog dialog) {
		this.dialog = dialog;
	}

	@Scheduled(cron = "${dialog.cleanup-cron:0 0 * * * *}")
	public void cleanUp() {
		int closed = dialog.closeExpired();
		if (closed > 0) {
			log.info("Очистка: закрыто {} (без ответа больше суток)",
					RussianPlural.format(closed, "брошенная анкета", "брошенные анкеты", "брошенных анкет"));
		}
	}
}
