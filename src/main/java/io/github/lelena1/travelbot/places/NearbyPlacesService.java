package io.github.lelena1.travelbot.places;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.lelena1.travelbot.city.CityDirectory;
import io.github.lelena1.travelbot.city.GeoPoint;
import io.github.lelena1.travelbot.city.KnownCity;
import io.github.lelena1.travelbot.integration.wiki.WikiException;
import io.github.lelena1.travelbot.integration.wiki.WikipediaClient;

/**
 * Куда съездить на день из города поездки: из Калининграда — в Зеленоградск и Светлогорск.
 * Список мест берётся из справочника (cities.yml), их достопримечательности — тем же способом,
 * что и для самого города.
 */
@Service
public class NearbyPlacesService {

	private static final Logger log = LoggerFactory.getLogger(NearbyPlacesService.class);

	static final int SIGHTS_PER_PLACE = 3;
	/** Места для поездки на день — небольшие городки: ищем в радиусе 4 км, а не 10, как для города. */
	static final int RADIUS_METERS = 4_000;
	/** «Куршская коса | Танцующий лес»: слева — как показывать, справа — статья, вокруг которой искать. */
	static final String DISPLAY_SEPARATOR = " | ";

	private final CityDirectory directory;
	private final CityLocator locator;
	private final SightsService sights;

	public NearbyPlacesService(CityDirectory directory, CityLocator locator, SightsService sights) {
		this.directory = directory;
		this.locator = locator;
		this.sights = sights;
	}

	public List<NearbyPlace> forCity(String destination, GeoPoint cityCenter) {
		List<String> titles = directory.find(destination).map(KnownCity::nearby).orElse(List.of());
		List<NearbyPlace> result = new ArrayList<>();
		for (String entry : titles) {
			String title = articleTitle(entry);
			try {
				Optional<GeoPoint> point = locator.locate(title);
				if (point.isEmpty()) {
					continue;
				}
				List<Sight> top = sights.topSights(title, point.get(), RADIUS_METERS).stream()
						.limit(SIGHTS_PER_PLACE).toList();
				result.add(new NearbyPlace(displayName(entry), WikipediaClient.articleUrl(title), point.get(),
						(int) Math.round(point.get().distanceTo(cityCenter) / 1000), top));
			} catch (WikiException e) {
				// одно недоступное место не должно лишать остальных
				log.warn("Рядом: не удалось загрузить «{}»: {}", title, e.getMessage());
			}
		}
		return result;
	}

	/** «Светлогорск (Калининградская область)» → «Светлогорск»; «Куршская коса | Танцующий лес» → «Куршская коса». */
	static String displayName(String entry) {
		int separator = entry.indexOf(DISPLAY_SEPARATOR);
		if (separator > 0) {
			return entry.substring(0, separator);
		}
		int bracket = entry.indexOf(" (");
		return bracket > 0 ? entry.substring(0, bracket) : entry;
	}

	/** «Куршская коса | Танцующий лес» → «Танцующий лес»; иначе вся строка — название статьи. */
	static String articleTitle(String entry) {
		int separator = entry.indexOf(DISPLAY_SEPARATOR);
		return separator > 0 ? entry.substring(separator + DISPLAY_SEPARATOR.length()) : entry;
	}

	/**
	 * Место для поездки на день.
	 *
	 * @param distanceKm расстояние от центра города по прямой
	 * @param sights     главные достопримечательности (может быть пусто — тогда просто сам город)
	 */
	public record NearbyPlace(String name, String articleUrl, GeoPoint point, int distanceKm, List<Sight> sights) {

		public NearbyPlace {
			sights = List.copyOf(sights);
		}
	}
}
