package io.github.lelena1.travelbot.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.dialog.NewTripDialog;
import io.github.lelena1.travelbot.flights.FlightSearchService;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripService;

class FlightsCommandTest {

	private static final long USER = 1;

	private TripService trips;
	private FlightSearchService search;
	private FlightsCommand command;

	@BeforeEach
	void setUp() {
		trips = mock(TripService.class);
		search = mock(FlightSearchService.class);
		command = new FlightsCommand(new TripChooser(trips), search);
		when(search.search(any())).thenReturn(new FlightSearchService.Problem("результат поиска"));
	}

	@Test
	void withoutTripsSuggestsCreatingOne() {
		when(trips.upcomingTrips(USER)).thenReturn(List.of());

		BotReply reply = command.menu(USER);

		assertThat(reply.text()).contains("предстоящих поездок пока нет").contains("Новая поездка");
		verify(search, never()).search(any());
	}

	@Test
	void singleTripIsSearchedRightAway() {
		Trip rome = trip("Рим", 5, 12);
		when(trips.upcomingTrips(USER)).thenReturn(List.of(rome));

		assertThat(command.menu(USER).text()).isEqualTo("результат поиска");
		verify(search).search(rome);
	}

	@Test
	void severalTripsOfferChoice() {
		when(trips.upcomingTrips(USER)).thenReturn(List.of(trip("Рим", 5, 12), trip("Казань", 7, 1)));

		BotReply reply = command.menu(USER);

		assertThat(reply.text()).contains("Для какой поездки");
		assertThat(reply.keyboard()).isEqualTo(Keyboard.options(List.of(
				List.of("✈️ 1. Рим · 12 мая"),
				List.of("✈️ 2. Казань · 1 июля"),
				List.of(NewTripDialog.CANCEL))));
	}

	@Test
	void choiceButtonIsRecognized() {
		assertThat(command.parseChoice("✈️ 2. Казань · 1 июля")).contains(2);
		assertThat(command.parseChoice("Казань")).isEmpty();
		assertThat(command.parseChoice(MainMenu.FLIGHTS.label())).isEmpty();
	}

	@Test
	void numberSearchesChosenTrip() {
		Trip kazan = trip("Казань", 7, 1);
		when(trips.upcomingTrips(USER)).thenReturn(List.of(trip("Рим", 5, 12), kazan));

		command.forNumber(USER, 2);

		verify(search).search(kazan);
	}

	@Test
	void unknownNumber() {
		when(trips.upcomingTrips(USER)).thenReturn(List.of(trip("Рим", 5, 12)));

		assertThat(command.forNumber(USER, 3).text()).contains("Такой поездки нет");
	}

	private static Trip trip(String destination, int month, int day) {
		Trip trip = Trip.draftFor(USER);
		trip.setDestination(destination);
		trip.setOrigin("Москва");
		LocalDate from = LocalDate.of(2027, month, day);
		trip.setDates(from, from.plusDays(5));
		return trip;
	}
}
