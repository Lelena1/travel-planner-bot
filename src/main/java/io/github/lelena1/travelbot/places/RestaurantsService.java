package io.github.lelena1.travelbot.places;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import io.github.lelena1.travelbot.city.GeoPoint;
import io.github.lelena1.travelbot.config.CacheConfig;
import io.github.lelena1.travelbot.integration.osm.OsmPlace;
import io.github.lelena1.travelbot.integration.osm.OverpassClient;

/**
 * Рестораны и кафе в центре города из OpenStreetMap.
 * <p>
 * Рейтингов в OpenStreetMap нет, поэтому первыми показываем заведения с самой подробной
 * информацией (кухня, часы работы, сайт, телефон, адрес) — обычно это известные и живые места.
 */
@Service
public class RestaurantsService {

	private static final Logger log = LoggerFactory.getLogger(RestaurantsService.class);

	static final int RADIUS_METERS = 1_500;
	static final int TOP = 8;
	static final List<String> INFO_TAGS = List.of("cuisine", "opening_hours", "website", "contact:website", "phone",
			"contact:phone", "addr:street");

	/** Самые частые значения тега cuisine по-русски. */
	static final Map<String, String> CUISINES = Map.ofEntries(
			Map.entry("russian", "русская"), Map.entry("georgian", "грузинская"), Map.entry("italian", "итальянская"),
			Map.entry("pizza", "пицца"), Map.entry("japanese", "японская"), Map.entry("sushi", "суши"),
			Map.entry("asian", "азиатская"), Map.entry("chinese", "китайская"), Map.entry("german", "немецкая"),
			Map.entry("czech", "чешская"), Map.entry("french", "французская"), Map.entry("european", "европейская"),
			Map.entry("international", "интернациональная"), Map.entry("uzbek", "узбекская"),
			Map.entry("caucasian", "кавказская"), Map.entry("ukrainian", "украинская"), Map.entry("tatar", "татарская"),
			Map.entry("burger", "бургеры"), Map.entry("seafood", "морепродукты"), Map.entry("fish", "рыба"),
			Map.entry("coffee_shop", "кофейня"), Map.entry("breakfast", "завтраки"), Map.entry("cake", "торты"),
			Map.entry("pie", "выпечка"), Map.entry("grill", "гриль"), Map.entry("steak_house", "стейки"),
			Map.entry("vegetarian", "вегетарианская"), Map.entry("vegan", "веганская"), Map.entry("turkish", "турецкая"),
			Map.entry("armenian", "армянская"), Map.entry("mediterranean", "средиземноморская"),
			Map.entry("thai", "тайская"), Map.entry("vietnamese", "вьетнамская"), Map.entry("korean", "корейская"),
			Map.entry("indian", "индийская"), Map.entry("mexican", "мексиканская"), Map.entry("spanish", "испанская"),
			Map.entry("greek", "греческая"), Map.entry("regional", "местная"));

	private final OverpassClient overpass;

	public RestaurantsService(OverpassClient overpass) {
		this.overpass = overpass;
	}

	/** Кешируется на неделю: запрос к OpenStreetMap занимает до минуты. */
	@Cacheable(cacheNames = CacheConfig.RESTAURANTS, key = "#cityName")
	public List<Restaurant> top(String cityName, GeoPoint center) {
		List<OsmPlace> places = overpass.restaurantsAndCafes(center, RADIUS_METERS);
		List<Restaurant> best = places.stream()
				.sorted(Comparator.comparingInt(RestaurantsService::infoScore).reversed()
						.thenComparing(place -> "restaurant".equals(place.amenity()) ? 0 : 1)
						.thenComparing(OsmPlace::name))
				.limit(TOP)
				.map(RestaurantsService::toRestaurant)
				.toList();
		log.info("Рестораны: {} — найдено {}, показываем {}", cityName, places.size(), best.size());
		return best;
	}

	static int infoScore(OsmPlace place) {
		return (int) INFO_TAGS.stream().filter(tag -> place.tag(tag) != null).count();
	}

	static Restaurant toRestaurant(OsmPlace place) {
		return new Restaurant(place.name(), "cafe".equals(place.amenity()), cuisine(place.tag("cuisine")),
				place.tag("opening_hours"), place.website(), place.point());
	}

	/** «georgian;grill» → «грузинская, гриль»; незнакомые значения пропускаем. */
	static String cuisine(String osmValue) {
		if (osmValue == null) {
			return null;
		}
		String translated = Arrays.stream(osmValue.split("[;,]"))
				.map(String::trim)
				.map(CUISINES::get)
				.filter(value -> value != null)
				.distinct()
				.collect(Collectors.joining(", "));
		return translated.isEmpty() ? null : translated;
	}
}
