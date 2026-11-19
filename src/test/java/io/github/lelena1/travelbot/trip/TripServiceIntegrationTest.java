package io.github.lelena1.travelbot.trip;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.lelena1.travelbot.TestcontainersConfiguration;
import io.github.lelena1.travelbot.user.AccessService;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TripServiceIntegrationTest {

	private static final long USER = 20_001;
	private static final long OTHER_USER = 20_002;

	@Autowired
	private TripService service;
	@Autowired
	private TripRepository trips;
	@Autowired
	private AccessService access;

	@Test
	void listsOnlyOwnActiveTripsSortedByDateAndDeletesByNumber() {
		access.allow(USER);
		access.allow(OTHER_USER);
		saveActive(USER, "Казань", LocalDate.of(2027, 7, 1));
		saveActive(USER, "Рим", LocalDate.of(2027, 5, 12));
		saveActive(OTHER_USER, "Париж", LocalDate.of(2027, 6, 1));
		trips.save(Trip.draftFor(USER)); // черновик в список не попадает

		assertThat(service.activeTrips(USER)).extracting(Trip::getDestination).containsExactly("Рим", "Казань");

		assertThat(service.deleteByNumber(USER, 2)).get().extracting(Trip::getDestination).isEqualTo("Казань");
		assertThat(service.deleteByNumber(USER, 5)).isEmpty();
		assertThat(service.activeTrips(USER)).extracting(Trip::getDestination).containsExactly("Рим");
		assertThat(service.activeTrips(OTHER_USER)).extracting(Trip::getDestination).containsExactly("Париж");
	}

	private void saveActive(long userId, String destination, LocalDate from) {
		Trip trip = Trip.draftFor(userId);
		trip.setDestination(destination);
		trip.setDates(from, from.plusDays(5));
		trip.activate();
		trips.save(trip);
	}
}
