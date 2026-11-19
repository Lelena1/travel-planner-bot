package io.github.lelena1.travelbot.flights;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.trip.Trip;

class PassengersTest {

	@Test
	void adultsOnly() {
		Passengers passengers = Passengers.of(trip(2, List.of()));

		assertThat(passengers).isEqualTo(new Passengers(2, 0, 0));
		assertThat(passengers.aviasalesSuffix()).isEqualTo("2");
	}

	@Test
	void childrenAreSplitByAirlineAgeRules() {
		Passengers passengers = Passengers.of(trip(2, List.of(1, 5, 14)));

		assertThat(passengers).isEqualTo(new Passengers(3, 1, 1));
		assertThat(passengers.total()).isEqualTo(5);
		assertThat(passengers.aviasalesSuffix()).isEqualTo("311");
	}

	@Test
	void atMostNinePerType() {
		assertThat(Passengers.of(trip(15, List.of())).adults()).isEqualTo(9);
	}

	private static Trip trip(int adults, List<Integer> childAges) {
		Trip trip = Trip.draftFor(1);
		trip.setAdults(adults);
		trip.setChildren(childAges.size());
		trip.setChildAges(childAges);
		return trip;
	}

	@Test
	void suffixMatchesAviasalesCanonicalForm() {
		assertThat(new Passengers(2, 1, 0).aviasalesSuffix()).isEqualTo("21");
		assertThat(new Passengers(2, 0, 1).aviasalesSuffix()).isEqualTo("201");
	}
}
