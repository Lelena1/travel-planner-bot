package io.github.lelena1.travelbot.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RussianPluralTest {

	@ParameterizedTest
	@CsvSource({
			"0, 0 пользователей",
			"1, 1 пользователь",
			"2, 2 пользователя",
			"4, 4 пользователя",
			"5, 5 пользователей",
			"11, 11 пользователей",
			"12, 12 пользователей",
			"14, 14 пользователей",
			"21, 21 пользователь",
			"22, 22 пользователя",
			"111, 111 пользователей",
			"101, 101 пользователь"
	})
	void choosesCorrectForm(long n, String expected) {
		assertThat(RussianPlural.format(n, "пользователь", "пользователя", "пользователей")).isEqualTo(expected);
	}
}
