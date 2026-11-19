package io.github.lelena1.travelbot.flights;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import io.github.lelena1.travelbot.integration.travelpayouts.Place;
import io.github.lelena1.travelbot.integration.travelpayouts.PlacesClient;
import io.github.lelena1.travelbot.util.CityNames;

/**
 * Превращает то, что пользователь написал в анкете («Италия, Рим», «Сочи»), в город с IATA-кодом.
 * <p>
 * Части через запятую проверяются с конца: обычно пишут «страна, город».
 * Если нашлась только страна, просим уточнить город — у страны нет одного аэропорта.
 */
@Service
public class CityResolver {

	private final PlacesClient places;

	public CityResolver(PlacesClient places) {
		this.places = places;
	}

	public Result resolve(String text) {
		List<String> parts = CityNames.partsFromEnd(text);

		Place country = null;
		for (String part : parts) {
			List<Place> found = places.search(part);
			Optional<Place> exact = found.stream()
					.filter(place -> normalize(place.name()).equals(normalize(part)))
					.findFirst();
			if (exact.isPresent() && exact.get().isCity()) {
				return new Found(exact.get(), part, true);
			}
			if (exact.isPresent() && exact.get().isCountry()) {
				country = country == null ? exact.get() : country;
				continue;
			}
			// Названия нет в справочнике, но вариант единственный — это ближайший аэропорт
			// (Кисловодск → Минеральные Воды). Если вариантов несколько, справочник просто
			// угадывает по буквам (Тверь → Тель-Авив), и такому ответу верить нельзя.
			if (found.size() == 1 && found.get(0).isCity()) {
				return new Found(found.get(0), part, false);
			}
		}
		return country != null ? new CountryOnly(country.name()) : new NotFound(text);
	}

	static String normalize(String name) {
		return CityNames.normalize(name);
	}

	public sealed interface Result permits Found, CountryOnly, NotFound {
	}

	/**
	 * @param city       город с аэропортом
	 * @param requested  как город был назван в поездке
	 * @param exactMatch false — в названном городе аэропорта нет, это ближайший
	 */
	public record Found(Place city, String requested, boolean exactMatch) implements Result {

		/** «Минеральные Воды (ближайший аэропорт к «Кисловодск»)» или просто «Сочи». */
		public String describe() {
			return exactMatch ? city.name() : "%s (ближайший аэропорт к «%s»)".formatted(city.name(), requested);
		}
	}

	/** Указана только страна, например «Италия». */
	public record CountryOnly(String country) implements Result {
	}

	public record NotFound(String text) implements Result {
	}
}
