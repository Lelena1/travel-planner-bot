package io.github.lelena1.travelbot.bot;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import io.github.lelena1.travelbot.dialog.NewTripDialog;
import io.github.lelena1.travelbot.flights.FlightFormatter;
import io.github.lelena1.travelbot.flights.FlightSearchService;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripService;

/**
 * Раздел «✈️ Авиабилеты»: выбор поездки и показ самых дешёвых билетов для неё.
 */
@Component
public class FlightsCommand {

	static final String CHOICE_PREFIX = "✈️ ";
	/** Кнопка выбора поездки: «✈️ 2. Рим · 12 мая». */
	private static final Pattern CHOICE = Pattern.compile("^✈️ (\\d{1,3})\\. .*");
	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"));

	private final TripService trips;
	private final FlightSearchService search;

	public FlightsCommand(TripService trips, FlightSearchService search) {
		this.trips = trips;
		this.search = search;
	}

	/** Кнопка меню или /flights без номера. */
	public BotReply menu(long userId) {
		List<Trip> upcoming = trips.upcomingTrips(userId);
		if (upcoming.isEmpty()) {
			return BotReply.withMenu("""
					✈️ Билеты я ищу для сохранённой поездки, а предстоящих поездок пока нет.
					Нажмите «%s», укажите откуда, куда и когда — и возвращайтесь сюда."""
					.formatted(MainMenu.NEW_TRIP.label()));
		}
		if (upcoming.size() == 1) {
			return searchFor(upcoming.get(0));
		}
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < upcoming.size(); i++) {
			rows.add(List.of(choiceLabel(i + 1, upcoming.get(i))));
		}
		rows.add(List.of(NewTripDialog.CANCEL));
		return BotReply.withOptions("✈️ Для какой поездки искать билеты?", rows);
	}

	/** /flights 2 — билеты для поездки с этим номером среди предстоящих. */
	public BotReply forNumber(long userId, int number) {
		List<Trip> upcoming = trips.upcomingTrips(userId);
		if (number < 1 || number > upcoming.size()) {
			return BotReply.withMenu("Такой поездки нет. Нажмите «%s», чтобы выбрать из списка."
					.formatted(MainMenu.FLIGHTS.label()));
		}
		return searchFor(upcoming.get(number - 1));
	}

	/** Номер поездки, если текст — нажатая кнопка выбора. */
	public Optional<Integer> parseChoice(String text) {
		Matcher matcher = CHOICE.matcher(text);
		return matcher.matches() ? Optional.of(Integer.parseInt(matcher.group(1))) : Optional.empty();
	}

	static String choiceLabel(int number, Trip trip) {
		return "%s%d. %s · %s".formatted(CHOICE_PREFIX, number, trip.getDestination(),
				trip.getDateFrom().format(DAY_MONTH));
	}

	private BotReply searchFor(Trip trip) {
		return FlightFormatter.toReply(search.search(trip), search::offerUrl);
	}
}
