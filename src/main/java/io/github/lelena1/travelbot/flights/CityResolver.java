package io.github.lelena1.travelbot.flights;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import io.github.lelena1.travelbot.integration.travelpayouts.Place;
import io.github.lelena1.travelbot.integration.travelpayouts.PlacesClient;

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
		List<String> parts = new ArrayList<>(Arrays.stream(text.split(","))
				.map(String::trim)
				.filter(part -> !part.isEmpty())
				.toList());
		Collections.reverse(parts);

		Place country = null;
		for (String part : parts) {
			List<Place> found = places.search(part);
			if (found.isEmpty()) {
				continue;
			}
			Place best = found.get(0);
			if (best.isCity()) {
				return new Found(best);
			}
			if (best.isCountry() && country == null) {
				country = best;
			}
		}
		return country != null ? new CountryOnly(country.name()) : new NotFound(text);
	}

	public sealed interface Result permits Found, CountryOnly, NotFound {
	}

	public record Found(Place city) implements Result {
	}

	/** Указана только страна, например «Италия». */
	public record CountryOnly(String country) implements Result {
	}

	public record NotFound(String text) implements Result {
	}
}
