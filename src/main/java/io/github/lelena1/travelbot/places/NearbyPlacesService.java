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
		for (String title : titles) {
			try {
				Optional<GeoPoint> point = locator.locate(title);
				if (point.isEmpty()) {
					continue;
				}
				List<Sight> top = sights.topSights(title, point.get()).stream().limit(SIGHTS_PER_PLACE).toList();
				result.add(new NearbyPlace(displayName(title), WikipediaClient.articleUrl(title), point.get(),
						(int) Math.round(point.get().distanceTo(cityCenter) / 1000), top));
			} catch (WikiException e) {
				// одно недоступное место не должно лишать остальных
				log.warn("Рядом: не удалось загрузить «{}»: {}", title, e.getMessage());
			}
		}
		return result;
	}

	/** «Светлогорск (Калининградская область)» → «Светлогорск». */
	static String displayName(String articleTitle) {
		int bracket = articleTitle.indexOf(" (");
		return bracket > 0 ? articleTitle.substring(0, bracket) : articleTitle;
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
