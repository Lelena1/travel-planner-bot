package io.github.lelena1.travelbot.places;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard.Link;
import io.github.lelena1.travelbot.city.CityDirectory;
import io.github.lelena1.travelbot.city.GeoPoint;
import io.github.lelena1.travelbot.city.KnownCity;
import io.github.lelena1.travelbot.integration.osm.OverpassClient.OverpassException;
import io.github.lelena1.travelbot.integration.wiki.WikiException;
import io.github.lelena1.travelbot.trip.Trip;

/** Путеводитель по городу поездки: что посмотреть, где поесть, какие экскурсии. */
@Service
public class CityGuideService {

	private static final Logger log = LoggerFactory.getLogger(CityGuideService.class);

	static final int INTRO_MAX_LENGTH = 220;
	static final String TRIPSTER = "https://experience.tripster.ru/experience/";

	private final CityLocator locator;
	private final SightsService sights;
	private final RestaurantsService restaurants;
	private final CityDirectory directory;

	public CityGuideService(CityLocator locator, SightsService sights, RestaurantsService restaurants,
			CityDirectory directory) {
		this.locator = locator;
		this.sights = sights;
		this.restaurants = restaurants;
		this.directory = directory;
	}

	/** 🏛 Главные достопримечательности с кнопками «Статья» и «На карте». */
	public BotReply sights(Trip trip) {
		String city = locator.cityName(trip.getDestination());
		try {
			Optional<GeoPoint> center = locator.locate(city);
			if (center.isEmpty()) {
				return cityNotFound(city);
			}
			List<Sight> found = sights.topSights(city, center.get());
			if (found.isEmpty()) {
				return BotReply.withMenu("🏛 В Википедии не нашлось известных достопримечательностей рядом с центром «%s»."
						.formatted(city));
			}
			StringBuilder text = new StringBuilder("🏛 Что посмотреть: ").append(city).append('\n');
			List<List<Link>> buttons = new ArrayList<>();
			for (int i = 0; i < found.size(); i++) {
				Sight sight = found.get(i);
				text.append('\n').append(i + 1).append(". ").append(sight.title());
				if (sight.description() != null && !sight.description().isBlank()) {
					text.append(" — ").append(sight.description());
				}
				if (sight.intro() != null) {
					text.append("\n   ").append(shorten(sight.intro()));
				}
				text.append('\n');
				buttons.add(List.of(new Link("📖 %d. %s".formatted(i + 1, sight.title()), sight.articleUrl()),
						new Link("🗺 Карта", sight.point().yandexMapsUrl())));
			}
			text.append("\nОписания — из Википедии, порядок — по известности места.");
			return BotReply.withLinks(text.toString(), buttons);
		} catch (WikiException e) {
			log.warn("Места: Википедия недоступна: {}", e.getMessage());
			return unavailable("Википедия");
		}
	}

	/** 🍽 Рестораны и кафе в центре с кнопками на Яндекс Карты. */
	public BotReply restaurants(Trip trip) {
		String city = locator.cityName(trip.getDestination());
		try {
			Optional<GeoPoint> center = locator.locate(city);
			if (center.isEmpty()) {
				return cityNotFound(city);
			}
			List<Restaurant> found = restaurants.top(city, center.get());
			Link allOnMap = new Link("🍽 Все рестораны рядом на Яндекс Картах",
					center.get().yandexMapsSearchUrl("рестораны"));
			if (found.isEmpty()) {
				return BotReply.withLinks("🍽 В OpenStreetMap не нашлось ресторанов в центре «%s». Посмотрите на карте:"
						.formatted(city), List.of(List.of(allOnMap)));
			}
			StringBuilder text = new StringBuilder("🍽 Где поесть в центре: ").append(city).append('\n');
			List<List<Link>> buttons = new ArrayList<>();
			for (int i = 0; i < found.size(); i++) {
				Restaurant place = found.get(i);
				text.append('\n').append(i + 1).append(". ").append(place.cafe() ? "☕ " : "🍴 ").append(place.name());
				if (place.cuisine() != null) {
					text.append(" — ").append(place.cuisine());
				}
				if (place.openingHours() != null) {
					text.append("\n   🕒 ").append(place.openingHours());
				}
				buttons.add(List.of(new Link("🗺 %d. %s".formatted(i + 1, place.name()),
						place.point().yandexMapsSearchUrl(place.name()))));
			}
			buttons.add(List.of(allOnMap));
			text.append("\n\nДанные — из OpenStreetMap. Рейтинги и отзывы смотрите на Яндекс Картах.");
			return BotReply.withLinks(text.toString(), buttons);
		} catch (WikiException | OverpassException e) {
			log.warn("Рестораны: сервис недоступен: {}", e.getMessage());
			return unavailable("OpenStreetMap");
		}
	}

	/** 🎟 Экскурсии на Трипстере. */
	public BotReply tours(Trip trip) {
		Optional<KnownCity> known = directory.find(trip.getDestination());
		String city = locator.cityName(trip.getDestination());
		Optional<String> tripster = known.flatMap(KnownCity::tripsterCity);
		if (tripster.isPresent()) {
			return BotReply.withLinks("""
					🎟 Экскурсии: %s
					Трипстер — экскурсии от местных гидов: пешие, автомобильные, на воде, с детьми.
					Выберите даты поездки на сайте, чтобы увидеть свободные экскурсии."""
					.formatted(city),
					List.of(List.of(new Link("🎟 Экскурсии на Трипстере", TRIPSTER + tripster.get() + "/"))));
		}
		return BotReply.withLinks("""
				🎟 Экскурсии: %s
				Для этого города у меня нет готовой ссылки. Откройте Трипстер и найдите «%s» в поиске."""
				.formatted(city, city),
				List.of(List.of(new Link("🎟 Трипстер", "https://experience.tripster.ru/"))));
	}

	static String shorten(String text) {
		if (text.length() <= INTRO_MAX_LENGTH) {
			return text;
		}
		int cut = text.lastIndexOf(' ', INTRO_MAX_LENGTH);
		return text.substring(0, cut > 0 ? cut : INTRO_MAX_LENGTH) + "…";
	}

	private static BotReply cityNotFound(String city) {
		return BotReply.withMenu("Не нашла город «%s» в Википедии. Проверьте название в поездке.".formatted(city));
	}

	private static BotReply unavailable(String service) {
		return BotReply.withMenu("😔 %s сейчас не отвечает. Попробуйте чуть позже.".formatted(service));
	}
}
