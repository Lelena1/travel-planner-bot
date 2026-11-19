package io.github.lelena1.travelbot.places;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import io.github.lelena1.travelbot.city.CityDirectory;
import io.github.lelena1.travelbot.city.GeoPoint;
import io.github.lelena1.travelbot.city.KnownCity;
import io.github.lelena1.travelbot.config.CacheConfig;
import io.github.lelena1.travelbot.integration.wiki.WikipediaClient;
import io.github.lelena1.travelbot.util.CityNames;

/**
 * Где находится город поездки: координаты центра по статье Википедии.
 * В отличие от справочника авиабилетов, здесь Кисловодск — это Кисловодск, а не ближайший аэропорт.
 */
@Service
public class CityLocator {

	private final CityDirectory directory;
	private final WikipediaClient wikipedia;

	public CityLocator(CityDirectory directory, WikipediaClient wikipedia) {
		this.directory = directory;
		this.wikipedia = wikipedia;
	}

	/** Название города из поездки: «Италия, Рим» → «Рим», «Питер» → «Санкт-Петербург». */
	public String cityName(String destination) {
		return directory.find(destination)
				.map(KnownCity::name)
				.orElseGet(() -> {
					List<String> parts = CityNames.partsFromEnd(destination);
					return parts.isEmpty() ? destination : parts.get(0);
				});
	}

	/**
	 * Координаты центра. Если статья с таким названием — не город (например, это страница
	 * значений), пробуем «Название (город)», как принято в Википедии.
	 */
	@Cacheable(CacheConfig.CITY_COORDINATES)
	public Optional<GeoPoint> locate(String cityName) {
		return wikipedia.coordinates(cityName).or(() -> wikipedia.coordinates(cityName + " (город)"));
	}
}
