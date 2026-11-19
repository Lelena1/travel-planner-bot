package io.github.lelena1.travelbot.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.dialog.NewTripDialog;
import io.github.lelena1.travelbot.lodging.LodgingService;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripService;

class LodgingCommandTest {

	private static final long USER = 1;

	private TripService trips;
	private LodgingService lodging;
	private LodgingCommand command;

	@BeforeEach
	void setUp() {
		trips = mock(TripService.class);
		lodging = mock(LodgingService.class);
		command = new LodgingCommand(new TripChooser(trips), lodging);
		when(lodging.forTrip(any())).thenReturn(BotReply.text("🏨 Жильё"));
	}

	@Test
	void singleTripShowsLodgingRightAway() {
		Trip kazan = trip("Казань", 7, 1);
		when(trips.upcomingTrips(USER)).thenReturn(List.of(kazan));

		assertThat(command.menu(USER).text()).isEqualTo("🏨 Жильё");
		verify(lodging).forTrip(kazan);
	}

	@Test
	void severalTripsOfferChoiceWithLodgingIcon() {
		when(trips.upcomingTrips(USER)).thenReturn(List.of(trip("Рим", 5, 12), trip("Казань", 7, 1)));

		BotReply reply = command.menu(USER);

		assertThat(reply.text()).isEqualTo("🏨 Для какой поездки искать жильё?");
		assertThat(reply.keyboard()).isEqualTo(Keyboard.options(List.of(
				List.of("🏨 1. Рим · 12 мая"), List.of("🏨 2. Казань · 1 июля"), List.of(NewTripDialog.CANCEL))));
	}

	@Test
	void lodgingChoiceIsNotConfusedWithFlightsChoice() {
		assertThat(command.parseChoice("🏨 2. Казань · 1 июля")).contains(2);
		assertThat(command.parseChoice("✈️ 2. Казань · 1 июля")).isEmpty();
	}

	private static Trip trip(String destination, int month, int day) {
		Trip trip = Trip.draftFor(USER);
		trip.setDestination(destination);
		LocalDate from = LocalDate.of(2027, month, day);
		trip.setDates(from, from.plusDays(5));
		return trip;
	}
}
