package io.github.lelena1.travelbot.dialog;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Понимает даты поездки в том виде, как их обычно пишут люди:
 * <ul>
 * <li>12.05–18.05, 12.05.2027 - 18.05.2027, 12/05-18/05</li>
 * <li>12 мая – 18 мая, 28 декабря - 5 января</li>
 * <li>12-18 мая</li>
 * </ul>
 * Если год не указан, берётся ближайшая будущая дата.
 */
@Component
public class DateRangeParser {

	static final int MAX_TRIP_DAYS = 90;
	static final int MAX_YEARS_AHEAD = 2;

	private static final Map<String, Integer> MONTHS = Map.ofEntries(
			Map.entry("янв", 1), Map.entry("фев", 2), Map.entry("мар", 3), Map.entry("апр", 4),
			Map.entry("мая", 5), Map.entry("май", 5), Map.entry("июн", 6), Map.entry("июл", 7),
			Map.entry("авг", 8), Map.entry("сен", 9), Map.entry("окт", 10), Map.entry("ноя", 11),
			Map.entry("дек", 12));

	private static final String MONTH_WORD = "(янв\\p{L}*|фев\\p{L}*|мар\\p{L}*|апр\\p{L}*|ма[йя]|июн\\p{L}*|июл\\p{L}*|авг\\p{L}*|сен\\p{L}*|окт\\p{L}*|ноя\\p{L}*|дек\\p{L}*)";

	/** «12-18 мая [2027]» — один месяц на оба дня. */
	private static final Pattern SAME_MONTH = Pattern.compile(
			"(\\d{1,2})\\s*[-–—]\\s*(\\d{1,2})\\s+" + MONTH_WORD + "(?:\\s+(\\d{4}))?");

	/** «12.05», «12.05.2027», «12/05/27». */
	private static final Pattern NUMERIC_DATE = Pattern.compile(
			"(?<!\\d)(\\d{1,2})[./](\\d{1,2})(?:[./](\\d{4}|\\d{2}))?(?!\\d)");

	/** «12 мая», «12 мая 2027». */
	private static final Pattern WORD_DATE = Pattern.compile(
			"(\\d{1,2})\\s+" + MONTH_WORD + "(?:\\s+(\\d{4}))?");

	private final Clock clock;

	public DateRangeParser(Clock clock) {
		this.clock = clock;
	}

	public Result parse(String input) {
		String text = input == null ? "" : input.toLowerCase(Locale.ROOT).trim();
		LocalDate today = LocalDate.now(clock);

		List<PartialDate> dates;
		try {
			dates = extractDates(text);
		} catch (DateTimeException | NumberFormatException e) {
			return new Failure("Такой даты не бывает. Проверьте числа и месяц.");
		}
		if (dates.size() != 2) {
			return new Failure("Не получилось понять даты. Напишите, например: 12.05–18.05 или 12–18 мая.");
		}

		LocalDate from;
		LocalDate to;
		try {
			from = dates.get(0).resolveNotBefore(today);
			to = dates.get(1).resolveNotBefore(from);
		} catch (DateTimeException e) {
			return new Failure("Такой даты не бывает. Проверьте числа и месяц.");
		}

		if (from.isBefore(today)) {
			return new Failure("Дата начала поездки уже прошла.");
		}
		if (to.isBefore(from)) {
			return new Failure("Дата окончания раньше даты начала.");
		}
		if (ChronoUnit.DAYS.between(from, to) > MAX_TRIP_DAYS) {
			return new Failure("Слишком длинная поездка: больше %d дней.".formatted(MAX_TRIP_DAYS));
		}
		if (from.isAfter(today.plusYears(MAX_YEARS_AHEAD))) {
			return new Failure("Так далеко вперёд я не планирую — не больше %d лет.".formatted(MAX_YEARS_AHEAD));
		}
		return new Success(from, to);
	}

	private List<PartialDate> extractDates(String text) {
		Matcher same = SAME_MONTH.matcher(text);
		if (same.find()) {
			int month = month(same.group(3));
			Integer year = year(same.group(4));
			return List.of(
					new PartialDate(Integer.parseInt(same.group(1)), month, year),
					new PartialDate(Integer.parseInt(same.group(2)), month, year));
		}

		List<PartialDate> result = new ArrayList<>();
		Matcher numeric = NUMERIC_DATE.matcher(text);
		while (numeric.find()) {
			result.add(new PartialDate(Integer.parseInt(numeric.group(1)),
					Integer.parseInt(numeric.group(2)), year(numeric.group(3))));
		}
		if (!result.isEmpty()) {
			return result;
		}

		Matcher word = WORD_DATE.matcher(text);
		while (word.find()) {
			result.add(new PartialDate(Integer.parseInt(word.group(1)), month(word.group(2)), year(word.group(3))));
		}
		return result;
	}

	private static int month(String word) {
		Integer month = MONTHS.get(word.substring(0, 3));
		if (month == null) {
			throw new DateTimeException("Unknown month: " + word);
		}
		return month;
	}

	private static Integer year(String text) {
		if (text == null) {
			return null;
		}
		int year = Integer.parseInt(text);
		return text.length() == 2 ? 2000 + year : year;
	}

	/** Дата, у которой может быть не указан год. */
	private record PartialDate(int day, int month, Integer year) {

		/** Если год не указан — ближайшая такая дата не раньше {@code notBefore}. */
		LocalDate resolveNotBefore(LocalDate notBefore) {
			if (year != null) {
				return LocalDate.of(year, month, day);
			}
			LocalDate candidate = LocalDate.of(notBefore.getYear(), month, day);
			return candidate.isBefore(notBefore) ? candidate.plusYears(1) : candidate;
		}
	}

	public sealed interface Result permits Success, Failure {
	}

	public record Success(LocalDate from, LocalDate to) implements Result {
	}

	public record Failure(String message) implements Result {
	}
}
