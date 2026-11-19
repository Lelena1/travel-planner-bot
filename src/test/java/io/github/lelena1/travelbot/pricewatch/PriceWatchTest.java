package io.github.lelena1.travelbot.pricewatch;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PriceWatchTest {

	private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

	@Test
	void minimumPriceIsTracked() {
		PriceWatch watch = new PriceWatch(1, 1, 10, "MOW", "ROM", "Москва → Рим", 50_000, NOW);

		watch.recordPrice(47_000, NOW.plusSeconds(3600));
		watch.recordPrice(49_000, NOW.plusSeconds(7200));

		assertThat(watch.getLastPrice()).isEqualTo(49_000);
		assertThat(watch.getMinPrice()).isEqualTo(47_000);
		assertThat(watch.getLastCheckedAt()).isEqualTo(NOW.plusSeconds(7200));
	}

	@Test
	void missingPriceDoesNotResetMinimum() {
		PriceWatch watch = new PriceWatch(1, 1, 10, "MOW", "ROM", "Москва → Рим", 50_000, NOW);

		watch.recordPrice(null, NOW.plusSeconds(3600));

		assertThat(watch.getLastPrice()).isNull();
		assertThat(watch.getMinPrice()).isEqualTo(50_000);
	}

	@Test
	void watchCanStartWithoutPrice() {
		PriceWatch watch = new PriceWatch(1, 1, 10, "MOW", "ROM", "Москва → Рим", null, NOW);

		assertThat(watch.getMinPrice()).isNull();
	}
}
