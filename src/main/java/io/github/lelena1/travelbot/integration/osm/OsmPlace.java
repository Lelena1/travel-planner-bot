package io.github.lelena1.travelbot.integration.osm;

import java.util.Map;

import io.github.lelena1.travelbot.city.GeoPoint;

/**
 * Заведение из OpenStreetMap.
 *
 * @param amenity restaurant или cafe
 * @param tags    все теги объекта: cuisine, opening_hours, website…
 */
public record OsmPlace(String name, String amenity, GeoPoint point, Map<String, String> tags) {

	public OsmPlace {
		tags = Map.copyOf(tags);
	}

	public String tag(String key) {
		return tags.get(key);
	}

	public String website() {
		return tags.getOrDefault("website", tags.get("contact:website"));
	}
}
