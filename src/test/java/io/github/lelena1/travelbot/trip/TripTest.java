package io.github.lelena1.travelbot.trip;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class TripTest {

	@Test
	void countsNightsBetweenDates() {
		Trip trip = Trip.draftFor(1);
		trip.setDates(LocalDate.of(2027, 5, 12), LocalDate.of(2027, 5, 18));

		assertThat(trip.nights()).isEqualTo(6);
	}

	@Test
	void countsAllTravelers() {
		Trip trip = Trip.draftFor(1);
		trip.setAdults(2);
		trip.setChildren(1);

		assertThat(trip.travelersCount()).isEqualTo(3);
	}

	@Test
	void newTripIsDraftUntilActivated() {
		Trip trip = Trip.draftFor(1);
		assertThat(trip.getStatus()).isEqualTo(TripStatus.DRAFT);

		trip.activate();
		assertThat(trip.getStatus()).isEqualTo(TripStatus.ACTIVE);
	}
}
