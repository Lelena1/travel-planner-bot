package io.github.lelena1.travelbot.integration.travelpayouts;

/** Настройки Travelpayouts для unit-тестов: ненастоящие адреса, чтобы тест не ушёл в интернет. */
public final class TestProperties {

	public static final TravelpayoutsProperties TRAVELPAYOUTS = new TravelpayoutsProperties(
			"test-token", "https://api.test", "https://autocomplete.test", "https://www.aviasales.ru");

	private TestProperties() {
	}
}
