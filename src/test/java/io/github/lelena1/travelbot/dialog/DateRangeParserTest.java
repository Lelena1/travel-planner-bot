package io.github.lelena1.travelbot.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.lelena1.travelbot.dialog.DateRangeParser.Failure;
import io.github.lelena1.travelbot.dialog.DateRangeParser.Result;
import io.github.lelena1.travelbot.dialog.DateRangeParser.Success;

class DateRangeParserTest {

	/** «Сегодня» в тестах — 19 ноября 2026. */
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

	private final DateRangeParser parser = new DateRangeParser(
			Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

	@ParameterizedTest(name = "«{0}» → {1} … {2}")
	@CsvSource(delimiter = '|', value = {
			"12.05–18.05              | 2027-05-12 | 2027-05-18",
			"12.05 - 18.05            | 2027-05-12 | 2027-05-18",
			"12.05.2027-18.05.2027    | 2027-05-12 | 2027-05-18",
			"12.05.27 — 18.05.27      | 2027-05-12 | 2027-05-18",
			"12/05-18/05              | 2027-05-12 | 2027-05-18",
			"с 12.05 по 18.05         | 2027-05-12 | 2027-05-18",
			"12 мая - 18 мая          | 2027-05-12 | 2027-05-18",
			"12-18 мая                | 2027-05-12 | 2027-05-18",
			"12–18 Мая 2028           | 2028-05-12 | 2028-05-18",
			"1 июня – 3 июля          | 2027-06-01 | 2027-07-03",
			"25.11-30.11              | 2026-11-25 | 2026-11-30",
			"19.11-20.11              | 2026-11-19 | 2026-11-20",
			"28.12-05.01              | 2026-12-28 | 2027-01-05",
			"28 декабря - 5 января    | 2026-12-28 | 2027-01-05"
	})
	void parsesCommonFormats(String input, LocalDate from, LocalDate to) {
		assertThat(parser.parse(input)).isEqualTo(new Success(from, to));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "в мае", "12.05", "завтра и послезавтра", "1.01 2.01 3.01" })
	void rejectsUnrecognizedInput(String input) {
		assertThat(failureMessage(parser.parse(input))).contains("Не получилось понять даты");
	}

	@Test
	void rejectsImpossibleDate() {
		assertThat(failureMessage(parser.parse("31.02-05.03"))).contains("Такой даты не бывает");
	}

	@Test
	void rejectsPastStartDate() {
		assertThat(failureMessage(parser.parse("01.11.2026-05.11.2026"))).contains("уже прошла");
	}

	@Test
	void rejectsEndBeforeStartWhenYearsGiven() {
		assertThat(failureMessage(parser.parse("18.05.2027-12.05.2027"))).contains("раньше даты начала");
	}

	@Test
	void rejectsTooLongTrip() {
		assertThat(failureMessage(parser.parse("01.01.2027-01.06.2027"))).contains("Слишком длинная");
	}

	@Test
	void rejectsTooFarAhead() {
		assertThat(failureMessage(parser.parse("01.01.2030-05.01.2030"))).contains("Так далеко");
	}

	private static String failureMessage(Result result) {
		assertThat(result).isInstanceOf(Failure.class);
		return ((Failure) result).message();
	}
}
