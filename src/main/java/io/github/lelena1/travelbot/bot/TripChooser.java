package io.github.lelena1.travelbot.bot;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import io.github.lelena1.travelbot.dialog.NewTripDialog;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripService;

/**
 * Выбор предстоящей поездки для раздела («✈️ Авиабилеты», «🏨 Жильё»):
 * одна поездка — сразу действие, несколько — кнопки «✈️ 1. Рим · 12 мая».
 */
@Component
public class TripChooser {

	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"));

	private final TripService trips;

	public TripChooser(TripService trips) {
		this.trips = trips;
	}

	/**
	 * @param section что за раздел: значок для кнопок, вопрос и что делать с выбранной поездкой
	 */
	public BotReply choose(long userId, Section section) {
		List<Trip> upcoming = trips.upcomingTrips(userId);
		if (upcoming.isEmpty()) {
			return BotReply.withMenu("""
					%s Нужна сохранённая поездка, а предстоящих поездок пока нет.
					Нажмите «%s», укажите откуда, куда и когда — и возвращайтесь сюда."""
					.formatted(section.icon(), MainMenu.NEW_TRIP.label()));
		}
		if (upcoming.size() == 1) {
			return section.action().apply(upcoming.get(0));
		}
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < upcoming.size(); i++) {
			rows.add(List.of(label(section, i + 1, upcoming.get(i))));
		}
		rows.add(List.of(NewTripDialog.CANCEL));
		return BotReply.withOptions(section.icon() + " " + section.question(), rows);
	}

	/** Поездка с этим номером среди предстоящих (кнопка выбора или «/flights 2»). */
	public BotReply forNumber(long userId, int number, Section section) {
		List<Trip> upcoming = trips.upcomingTrips(userId);
		if (number < 1 || number > upcoming.size()) {
			return BotReply.withMenu("Такой поездки нет. Нажмите «%s», чтобы выбрать из списка."
					.formatted(section.menuItem().label()));
		}
		return section.action().apply(upcoming.get(number - 1));
	}

	/** Номер поездки, если текст — нажатая кнопка выбора этого раздела. */
	public Optional<Integer> parseChoice(String text, Section section) {
		Matcher matcher = Pattern.compile("^" + Pattern.quote(section.icon()) + " (\\d{1,3})\\. .*").matcher(text);
		return matcher.matches() ? Optional.of(Integer.parseInt(matcher.group(1))) : Optional.empty();
	}

	static String label(Section section, int number, Trip trip) {
		return "%s %d. %s · %s".formatted(section.icon(), number, trip.getDestination(),
				trip.getDateFrom().format(DAY_MONTH));
	}

	/**
	 * @param icon     значок раздела в начале кнопок выбора — по нему бот понимает, какой раздел выбран
	 * @param question вопрос при нескольких поездках
	 * @param menuItem кнопка главного меню раздела
	 * @param action   что сделать с выбранной поездкой
	 */
	public record Section(String icon, String question, MainMenu menuItem, Function<Trip, BotReply> action) {
	}
}
