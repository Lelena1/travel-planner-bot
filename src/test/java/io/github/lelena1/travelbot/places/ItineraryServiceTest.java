package io.github.lelena1.travelbot.places;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard;
import io.github.lelena1.travelbot.city.GeoPoint;
import io.github.lelena1.travelbot.integration.osm.OverpassClient.OverpassException;
import io.github.lelena1.travelbot.trip.Trip;

class ItineraryServiceTest {

	private static final GeoPoint CENTER = new GeoPoint(54.71, 20.51);
	private static final Sight CATHEDRAL = sight("Кёнигсбергский собор", 54.7064, 20.5118);
	private static final Sight CASTLE = sight("Кёнигсбергский замок", 54.7100, 20.5106);
	private static final Sight ZOO = sight("Калининградский зоопарк", 54.7204, 20.4874);
	private static final Restaurant FISCHER = new Restaurant("Тётка Фишер", false, "немецкая", null, null,
			new GeoPoint(54.7080, 20.5110));

	private CityLocator locator;
	private SightsService sights;
	private RestaurantsService restaurants;
	private ItineraryService service;

	@BeforeEach
	void setUp() {
		locator = mock(CityLocator.class);
		sights = mock(SightsService.class);
		restaurants = mock(RestaurantsService.class);
		service = new ItineraryService(locator, sights, restaurants, new ItineraryPlanner());
		when(locator.cityName(any())).thenReturn("Калининград");
		when(locator.locate("Калининград")).thenReturn(Optional.of(CENTER));
		when(sights.topSights("Калининград", CENTER)).thenReturn(List.of(CATHEDRAL, ZOO, CASTLE));
		when(restaurants.top("Калининград", CENTER)).thenReturn(List.of(FISCHER));
	}

	@Test
	void twoDayPlanWithWalksLunchAndRoutes() {
		BotReply reply = service.forTrip(trip(LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 28)));

		assertThat(reply.text())
				.startsWith("🗓 План поездки: Калининград\n27 ноября – 28 ноября 2026 (1 ночь) · 2 дня")
				.contains("День 1 · пт, 27 ноября\n🏛 Кёнигсбергский собор\n   🚶 ")
				.contains("🍽 Обед рядом: Тётка Фишер — немецкая")
				.contains("День 2 · сб, 28 ноября");
		List<List<Keyboard.Link>> rows = ((Keyboard.Links) reply.keyboard()).rows();
		assertThat(rows).extracting(row -> row.get(0).text()).containsExactly("🗺 Маршрут дня 1", "🗺 Маршрут дня 2");
		assertThat(rows.get(0).get(0).url()).startsWith("https://yandex.ru/maps/?rtext=54.706400,20.511800~").endsWith("&rtt=pd");
	}

	@Test
	void restaurantsDownStillGivesPlanWithoutLunch() {
		when(restaurants.top(any(), any())).thenThrow(new OverpassException("down", null));

		BotReply reply = service.forTrip(trip(LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 29)));

		assertThat(reply.text()).contains("День 1").doesNotContain("Обед");
	}

	@Test
	void longTripIsPlannedForOneWeek() {
		BotReply reply = service.forTrip(trip(LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 15)));

		assertThat(reply.text()).contains("15 дней").contains("План — на первые 7 дней").contains("День 7")
				.doesNotContain("День 8").contains("🌿 Свободный день");
	}

	@Test
	void walkingDistanceWording() {
		assertThat(ItineraryService.walk(654)).isEqualTo("🚶 650 м");
		assertThat(ItineraryService.walk(1240)).isEqualTo("🚶 1,2 км");
		assertThat(ItineraryService.walk(4500)).isEqualTo("🚕 4,5 км — удобнее на транспорте");
	}

	private static Sight sight(String title, double lat, double lon) {
		return new Sight(title, null, null, new GeoPoint(lat, lon), "https://ru.wikipedia.org/wiki/x", 10);
	}

	private static Trip trip(LocalDate from, LocalDate to) {
		Trip trip = Trip.draftFor(1);
		trip.setDestination("Калининград");
		trip.setDates(from, to);
		return trip;
	}
}
