package io.github.lelena1.travelbot.trip;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;

/** Бюджет на жильё за ночь, в рублях. */
public enum Budget {

	UP_TO_3000("до 3 000 ₽", 3_000),
	UP_TO_6000("до 6 000 ₽", 6_000),
	UP_TO_10000("до 10 000 ₽", 10_000),
	ANY("Неважно", null);

	private final String label;
	private final Integer maxPerNight;

	Budget(String label, Integer maxPerNight) {
		this.label = label;
		this.maxPerNight = maxPerNight;
	}

	public String label() {
		return label;
	}

	/** Верхняя граница цены за ночь; пусто, если бюджет не ограничен. */
	public OptionalInt maxPerNight() {
		return maxPerNight == null ? OptionalInt.empty() : OptionalInt.of(maxPerNight);
	}

	public static Optional<Budget> byLabel(String text) {
		return Arrays.stream(values()).filter(b -> b.label.equalsIgnoreCase(text)).findFirst();
	}
}
