package io.github.lelena1.travelbot.integration.travelpayouts;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Город или страна из справочника Travelpayouts.
 *
 * @param code        IATA-код города (ROM, MOW) или страны (IT)
 * @param name        название по-русски
 * @param type        "city" или "country"
 * @param countryName страна (для городов)
 * @param cases       падежи: vi — «в Рим», ro — «Рима», da — «Риму» и т.д.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Place(
		String code,
		String name,
		String type,
		@JsonProperty("country_name") String countryName,
		Map<String, String> cases) {

	public boolean isCity() {
		return "city".equals(type);
	}

	public boolean isCountry() {
		return "country".equals(type);
	}

	/** «в Рим» — для фраз вроде «Билеты в Рим». */
	public String toDirection() {
		return caseOrDefault("vi", "в " + name);
	}

	/** «Рима» — для фраз вроде «из Рима». */
	public String genitive() {
		return caseOrDefault("ro", name);
	}

	private String caseOrDefault(String key, String fallback) {
		if (cases == null) {
			return fallback;
		}
		String value = cases.get(key);
		return value == null || value.isBlank() ? fallback : value;
	}
}
