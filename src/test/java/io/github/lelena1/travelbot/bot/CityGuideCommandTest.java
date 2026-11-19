package io.github.lelena1.travelbot.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.places.CityGuideService;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripService;

class CityGuideCommandTest {

	private static final long USER = 1;

	private TripService trips;
	private CityGuideService guide;
	private CityGuideCommand command;
	private Trip rome;
	private Trip kazan;

	@BeforeEach
	void setUp() {
		trips = mock(TripService.class);
		guide = mock(CityGuideService.class);
		command = new CityGuideCommand(new TripChooser(trips), guide);
		rome = trip("Рим", 5, 12);
		kazan = trip("Казань", 7, 1);
		when(guide.sights(rome)).thenReturn(BotReply.text("🏛 Рим"));
		when(guide.restaurants(kazan)).thenReturn(BotReply.text("🍽 Казань"));
		when(guide.tours(rome)).thenReturn(BotReply.text("🎟 Рим"));
	}

	@Test
	void menuButtonWithSingleTripAnswersRightAway() {
		when(trips.upcomingTrips(USER)).thenReturn(List.of(rome));

		assertThat(command.menu(USER, MainMenu.PLACES)).get().extracting(BotReply::text).isEqualTo("🏛 Рим");
		assertThat(command.menu(USER, MainMenu.TOURS)).get().extracting(BotReply::text).isEqualTo("🎟 Рим");
		assertThat(command.menu(USER, MainMenu.FLIGHTS)).isEmpty();
	}

	@Test
	void choiceButtonsKeepTheirSection() {
		when(trips.upcomingTrips(USER)).thenReturn(List.of(rome, kazan));

		assertThat(command.menu(USER, MainMenu.RESTAURANTS)).get().extracting(BotReply::text)
				.isEqualTo("🍽 Для какой поездки искать рестораны?");
		assertThat(command.choice(USER, "🍽 2. Казань · 1 июля")).get().extracting(BotReply::text).isEqualTo("🍽 Казань");
		assertThat(command.choice(USER, "✈️ 2. Казань · 1 июля")).isEmpty();
	}

	@Test
	void commandsWithAndWithoutNumber() {
		when(trips.upcomingTrips(USER)).thenReturn(List.of(rome, kazan));

		assertThat(command.command(USER, "/tours", Optional.of(1L))).get().extracting(BotReply::text).isEqualTo("🎟 Рим");
		assertThat(command.command(USER, "/places", Optional.empty())).get().extracting(BotReply::text)
				.isEqualTo("🏛 Для какой поездки показать достопримечательности?");
		assertThat(command.command(USER, "/help", Optional.empty())).isEmpty();
	}

	private static Trip trip(String destination, int month, int day) {
		Trip trip = Trip.draftFor(USER);
		trip.setDestination(destination);
		LocalDate from = LocalDate.of(2027, month, day);
		trip.setDates(from, from.plusDays(5));
		return trip;
	}
}
