package io.github.lelena1.travelbot.flights;

import java.util.List;

import io.github.lelena1.travelbot.trip.Trip;

/**
 * Пассажиры по правилам авиакомпаний: взрослые — от 12 лет, дети — 2–11, младенцы — до 2 лет.
 */
public record Passengers(int adults, int children, int infants) {

	/** Больше 9 пассажиров одного типа поиск Aviasales не принимает. */
	static final int MAX_PER_TYPE = 9;

	public static Passengers of(Trip trip) {
		int adults = trip.getAdults() == null ? 1 : trip.getAdults();
		int children = 0;
		int infants = 0;
		List<Integer> ages = trip.getChildAges();
		for (int age : ages) {
			if (age >= 12) {
				adults++;
			} else if (age >= 2) {
				children++;
			} else {
				infants++;
			}
		}
		return new Passengers(Math.min(adults, MAX_PER_TYPE), Math.min(children, MAX_PER_TYPE),
				Math.min(infants, MAX_PER_TYPE));
	}

	public int total() {
		return adults + children + infants;
	}

	/** Хвост адреса поиска Aviasales: «2» — двое взрослых, «210» — двое взрослых и ребёнок. */
	String aviasalesSuffix() {
		if (children == 0 && infants == 0) {
			return String.valueOf(adults);
		}
		return "" + adults + children + infants;
	}
}
