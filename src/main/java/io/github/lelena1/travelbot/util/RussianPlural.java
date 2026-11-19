package io.github.lelena1.travelbot.util;

/** Склонение существительных после числительных: 1 поездка, 2 поездки, 5 поездок. */
public final class RussianPlural {

	private RussianPlural() {
	}

	/**
	 * @param n    число
	 * @param one  форма для 1, 21, 31… (поездка)
	 * @param few  форма для 2–4, 22–24… (поездки)
	 * @param many форма для 0, 5–20, 25–30… (поездок)
	 * @return например, «3 поездки»
	 */
	public static String format(long n, String one, String few, String many) {
		return n + " " + choose(n, one, few, many);
	}

	public static String choose(long n, String one, String few, String many) {
		long abs = Math.abs(n);
		long lastTwo = abs % 100;
		long last = abs % 10;
		if (lastTwo >= 11 && lastTwo <= 14) {
			return many;
		}
		if (last == 1) {
			return one;
		}
		if (last >= 2 && last <= 4) {
			return few;
		}
		return many;
	}
}
