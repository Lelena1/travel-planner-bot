package io.github.lelena1.travelbot.bot;

import java.util.Optional;

import org.springframework.stereotype.Component;

import io.github.lelena1.travelbot.lodging.LodgingService;

/** Раздел «🏨 Жильё»: выбор поездки и ссылки на поиск жилья с её условиями. */
@Component
public class LodgingCommand {

	private final TripChooser chooser;
	private final TripChooser.Section section;

	public LodgingCommand(TripChooser chooser, LodgingService lodging) {
		this.chooser = chooser;
		this.section = new TripChooser.Section("🏨", "Для какой поездки искать жильё?", MainMenu.LODGING,
				lodging::forTrip);
	}

	public BotReply menu(long userId) {
		return chooser.choose(userId, section);
	}

	public BotReply forNumber(long userId, int number) {
		return chooser.forNumber(userId, number, section);
	}

	/** Номер поездки, если текст — нажатая кнопка выбора «🏨 2. Рим · 12 мая». */
	public Optional<Integer> parseChoice(String text) {
		return chooser.parseChoice(text, section);
	}
}
