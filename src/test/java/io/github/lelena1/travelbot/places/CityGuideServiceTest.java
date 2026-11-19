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
import io.github.lelena1.travelbot.city.CityDirectory;
import io.github.lelena1.travelbot.city.GeoPoint;
import io.github.lelena1.travelbot.integration.wiki.WikiException;
import io.github.lelena1.travelbot.trip.Trip;

class CityGuideServiceTest {

	private static final GeoPoint CENTER = new GeoPoint(54.71667, 20.5);

	private CityLocator locator;
	private SightsService sights;
	private RestaurantsService restaurants;
	private NearbyPlacesService nearby;
	private CityGuideService guide;

	@BeforeEach
	void setUp() {
		locator = mock(CityLocator.class);
		sights = mock(SightsService.class);
		restaurants = mock(RestaurantsService.class);
		nearby = mock(NearbyPlacesService.class);
		guide = new CityGuideService(locator, sights, restaurants, new CityDirectory(), nearby);
		when(locator.cityName(any())).thenAnswer(call -> {
			String destination = call.getArgument(0);
			return destination.contains(",") ? destination.substring(destination.lastIndexOf(',') + 1).trim() : destination;
		});
		when(locator.locate("Калининград")).thenReturn(Optional.of(CENTER));
	}

	@Test
	void sightsWithArticleAndMapButtons() {
		when(sights.topSights("Калининград", CENTER)).thenReturn(List.of(
				new Sight("Кёнигсбергский собор", "собор в Калининграде", "Кафедральный собор — готический храм.",
						new GeoPoint(54.7062, 20.5112), "https://ru.wikipedia.org/wiki/x", 32)));

		BotReply reply = guide.sights(trip("Калининград"));

		assertThat(reply.text()).startsWith("🏛 Что посмотреть: Калининград")
				.contains("1. Кёнигсбергский собор — собор в Калининграде\n   Кафедральный собор — готический храм.");
		List<Keyboard.Link> row = ((Keyboard.Links) reply.keyboard()).rows().get(0);
		assertThat(row.get(0).text()).isEqualTo("📖 1. Кёнигсбергский собор");
		assertThat(row.get(1).url()).isEqualTo("https://yandex.ru/maps/?pt=20.511200,54.706200&z=16&l=map");
	}

	@Test
	void unknownCityInWikipedia() {
		when(locator.locate("Хогвартс")).thenReturn(Optional.empty());

		assertThat(guide.sights(trip("Хогвартс")).text()).contains("Не нашла город «Хогвартс»");
	}

	@Test
	void wikipediaDownIsReportedPolitely() {
		when(sights.topSights(any(), any())).thenThrow(new WikiException("timeout", null));

		assertThat(guide.sights(trip("Калининград")).text()).contains("Википедия сейчас не отвечает");
	}

	@Test
	void restaurantsWithCuisineHoursAndMapLinks() {
		when(restaurants.top("Калининград", CENTER)).thenReturn(List.of(
				new Restaurant("Тётка Фишер", false, "немецкая", "10:00-24:00", null, new GeoPoint(54.71, 20.51)),
				new Restaurant("Штолле", true, null, null, null, new GeoPoint(54.72, 20.5))));

		BotReply reply = guide.restaurants(trip("Калининград"));

		assertThat(reply.text())
				.contains("1. 🍴 Тётка Фишер — немецкая\n   🕒 10:00-24:00")
				.contains("2. ☕ Штолле");
		List<List<Keyboard.Link>> rows = ((Keyboard.Links) reply.keyboard()).rows();
		assertThat(rows).hasSize(3);
		assertThat(rows.get(0).get(0).url()).startsWith("https://yandex.ru/maps/?text=%D0%A2%D1%91%D1%82%D0%BA%D0%B0%20");
		assertThat(rows.get(2).get(0).text()).contains("Все рестораны рядом");
	}

	@Test
	void toursLinkToTripsterCityPage() {
		BotReply reply = guide.tours(trip("Россия, Калининград"));

		assertThat(reply.text()).startsWith("🎟 Экскурсии: Калининград");
		assertThat(((Keyboard.Links) reply.keyboard()).rows().get(0).get(0).url())
				.isEqualTo("https://experience.tripster.ru/experience/Kaliningrad/");
	}

	@Test
	void toursForUnknownCitySuggestSearch() {
		BotReply reply = guide.tours(trip("Великие Луки"));

		assertThat(reply.text()).contains("найдите «Великие Луки» в поиске");
	}

	@Test
	void longIntroIsShortenedOnWordBoundary() {
		String intro = "слово ".repeat(60);

		String shortened = CityGuideService.shorten(intro);

		assertThat(shortened).endsWith("слово…").hasSizeLessThanOrEqualTo(CityGuideService.INTRO_MAX_LENGTH + 1);
	}

	private static Trip trip(String destination) {
		Trip trip = Trip.draftFor(1);
		trip.setDestination(destination);
		trip.setDates(LocalDate.of(2027, 5, 12), LocalDate.of(2027, 5, 18));
		return trip;
	}

	@Test
	void openStreetMapDownStillGivesMapLinks() {
		when(restaurants.top("Калининград", CENTER))
				.thenThrow(new io.github.lelena1.travelbot.integration.osm.OverpassClient.OverpassException("down", null));

		BotReply reply = guide.restaurants(trip("Калининград"));

		assertThat(reply.text()).contains("не загрузился");
		assertThat(((Keyboard.Links) reply.keyboard()).rows()).extracting(row -> row.get(0).text())
				.containsExactly("🍽 Все рестораны рядом на Яндекс Картах", "☕ Кафе рядом на Яндекс Картах");
	}

	@Test
	void sightsIncludeDayTripsNearby() {
		when(sights.topSights("Калининград", CENTER)).thenReturn(List.of(
				new Sight("Кёнигсбергский собор", null, null, new GeoPoint(54.7062, 20.5112), "https://ru.wikipedia.org/wiki/x", 32)));
		when(nearby.forCity("Калининград", CENTER)).thenReturn(List.of(
				new NearbyPlacesService.NearbyPlace("Зеленоградск", "https://ru.wikipedia.org/wiki/z", new GeoPoint(54.95, 20.48), 26,
						List.of(new Sight("Зеленоградский променад", null, null, new GeoPoint(54.96, 20.47), "u", 5),
								new Sight("Кирха Святого Адальберта", null, null, new GeoPoint(54.95, 20.48), "u", 4))),
				new NearbyPlacesService.NearbyPlace("Светлогорск", "https://ru.wikipedia.org/wiki/s", new GeoPoint(54.94, 20.16), 23,
						List.of())));

		BotReply reply = guide.sights(trip("Калининград"));

		assertThat(reply.text()).contains("""
				🚗 Рядом, на день:
				• Зеленоградск (~26 км): Зеленоградский променад, Кирха Святого Адальберта
				• Светлогорск (~23 км)""");
		List<List<Keyboard.Link>> rows = ((Keyboard.Links) reply.keyboard()).rows();
		assertThat(rows.get(rows.size() - 1)).extracting(Keyboard.Link::text).containsExactly("🚗 Зеленоградск", "🚗 Светлогорск");
	}
}
