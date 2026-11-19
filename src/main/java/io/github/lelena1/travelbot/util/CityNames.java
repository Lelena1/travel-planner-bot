package io.github.lelena1.travelbot.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Работа с названиями городов, как их пишут пользователи. */
public final class CityNames {

	private CityNames() {
	}

	/** Для сравнения: «Ростов-на-Дону» = «ростов на  дону», «Орёл» = «орел». */
	public static String normalize(String name) {
		return name.toLowerCase(Locale.ROOT)
				.replace('ё', 'е')
				.replace('-', ' ')
				.replaceAll("\\s+", " ")
				.trim();
	}

	/**
	 * Части через запятую, начиная с последней: обычно пишут «страна, город»,
	 * поэтому город чаще всего в конце. «Италия, Рим» → [Рим, Италия].
	 */
	public static List<String> partsFromEnd(String text) {
		List<String> parts = new ArrayList<>(Arrays.stream(text.split(","))
				.map(String::trim)
				.filter(part -> !part.isEmpty())
				.toList());
		Collections.reverse(parts);
		return parts;
	}
}
