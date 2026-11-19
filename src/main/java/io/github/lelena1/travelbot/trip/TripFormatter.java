package io.github.lelena1.travelbot.trip;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import io.github.lelena1.travelbot.util.RussianPlural;

/** Человекочитаемое описание поездки для сообщений бота. */
public final class TripFormatter {

	private static final Locale RU = Locale.forLanguageTag("ru");
	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM", RU);
	private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMMM yyyy", RU);

	private TripFormatter() {
	}

	public static String summary(Trip trip) {
		List<String> lines = new ArrayList<>();
		lines.add("📍 " + trip.getDestination());
		if (trip.getOrigin() != null) {
			lines.add("🛫 Откуда: " + trip.getOrigin());
		}
		if (trip.getDateFrom() != null) {
			lines.add("🗓 " + summaryDates(trip));
		}
		if (trip.getAdults() != null) {
			lines.add("👥 " + travelers(trip));
		}
		if (trip.getPets() != null) {
			lines.add("🐾 " + pets(trip.getPets()));
		}
		if (trip.getBudget() != null) {
			lines.add("💰 " + budget(trip.getBudget()));
		}
		return String.join("\n", lines);
	}

	/** «12 мая – 18 мая 2027 (6 ночей)». */
	public static String summaryDates(Trip trip) {
		return dates(trip.getDateFrom(), trip.getDateTo())
				+ " (" + RussianPlural.format(trip.nights(), "ночь", "ночи", "ночей") + ")";
	}

	/** «12 – 18 мая 2027», но «28 декабря 2026 – 5 января 2027». */
	static String dates(LocalDate from, LocalDate to) {
		if (from.getYear() != to.getYear()) {
			return from.format(DAY_MONTH_YEAR) + " – " + to.format(DAY_MONTH_YEAR);
		}
		return from.format(DAY_MONTH) + " – " + to.format(DAY_MONTH_YEAR);
	}

	static String travelers(Trip trip) {
		String result = RussianPlural.format(trip.getAdults(), "взрослый", "взрослых", "взрослых");
		int children = trip.getChildren() == null ? 0 : trip.getChildren();
		if (children > 0) {
			result += ", " + RussianPlural.format(children, "ребёнок", "ребёнка", "детей");
			if (!trip.getChildAges().isEmpty()) {
				result += " (" + childAges(trip.getChildAges()) + ")";
			}
		}
		return result;
	}

	private static String childAges(List<Integer> ages) {
		String numbers = ages.stream().map(String::valueOf).collect(Collectors.joining(", "));
		int last = ages.get(ages.size() - 1);
		return numbers + " " + RussianPlural.choose(last, "год", "года", "лет");
	}

	static String pets(Pets pets) {
		return switch (pets) {
			case NONE -> "Без животных";
			case DOG -> "С собакой";
			case CAT -> "С кошкой";
			case OTHER -> "С питомцем";
		};
	}

	static String budget(Budget budget) {
		return budget == Budget.ANY ? "Бюджет на жильё не ограничен" : "Жильё " + budget.label() + " за ночь";
	}
}
