package io.github.lelena1.travelbot.pricewatch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PriceChangeTest {

	@ParameterizedTest(name = "{0} → {1}: {2}")
	@CsvSource(nullValues = "null", value = {
			"null,  47948, APPEARED",
			"47948, null,  NONE",
			"null,  null,  NONE",
			"47948, 45000, DROPPED",   // −6%
			"47948, 47000, NONE",      // −2%: мелкое колебание
			"47948, 49000, NONE",      // подорожало
			"8000,  7750,  NONE",      // −3.1%, но всего 250 ₽
			"8000,  7600,  DROPPED",   // −5% и 400 ₽
			"10000, 9700,  DROPPED"    // ровно 3% и ровно 300 ₽
	})
	void decidesWhetherToNotify(Integer previous, Integer current, PriceChange expected) {
		assertThat(PriceChange.of(previous, current)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({ "47948, 45000, 6", "10000, 9700, 3", "10000, 5000, 50" })
	void dropPercentRoundsDown(int previous, int current, int percent) {
		assertThat(PriceChange.dropPercent(previous, current)).isEqualTo(percent);
	}
}
