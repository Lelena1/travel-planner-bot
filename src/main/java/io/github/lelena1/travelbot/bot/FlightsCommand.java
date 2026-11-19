package io.github.lelena1.travelbot.bot;

import java.util.Optional;

import org.springframework.stereotype.Component;

import io.github.lelena1.travelbot.flights.FlightFormatter;
import io.github.lelena1.travelbot.flights.FlightSearchService;

/** Раздел «✈️ Авиабилеты»: выбор поездки и самые дешёвые билеты для неё. */
@Component
public class FlightsCommand {

	private final TripChooser chooser;
	private final TripChooser.Section section;

	public FlightsCommand(TripChooser chooser, FlightSearchService search) {
		this.chooser = chooser;
		this.section = new TripChooser.Section("✈️", "Для какой поездки искать билеты?", MainMenu.FLIGHTS,
				trip -> FlightFormatter.toReply(search.search(trip), search::offerUrl));
	}

	/** Кнопка меню или /flights без номера. */
	public BotReply menu(long userId) {
		return chooser.choose(userId, section);
	}

	/** /flights 2 — билеты для поездки с этим номером среди предстоящих. */
	public BotReply forNumber(long userId, int number) {
		return chooser.forNumber(userId, number, section);
	}

	/** Номер поездки, если текст — нажатая кнопка выбора «✈️ 2. Рим · 12 мая». */
	public Optional<Integer> parseChoice(String text) {
		return chooser.parseChoice(text, section);
	}
}
