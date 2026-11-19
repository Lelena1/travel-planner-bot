package io.github.lelena1.travelbot.trip;

import java.util.Arrays;
import java.util.Optional;

/** Едут ли с путешественниками питомцы. Важно при поиске жилья. */
public enum Pets {

	NONE("Нет"),
	DOG("🐕 Собака"),
	CAT("🐈 Кошка"),
	OTHER("🐾 Другое");

	private final String label;

	Pets(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public boolean any() {
		return this != NONE;
	}

	public static Optional<Pets> byLabel(String text) {
		return Arrays.stream(values()).filter(p -> p.label.equalsIgnoreCase(text)).findFirst();
	}
}
