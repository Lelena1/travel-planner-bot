package io.github.lelena1.travelbot.places;

import io.github.lelena1.travelbot.city.GeoPoint;

/**
 * Ресторан или кафе.
 *
 * @param cuisine      кухня по-русски («грузинская, гриль»); может быть пустой
 * @param openingHours часы работы в формате OpenStreetMap («Mo-Su 12:00-24:00»); может быть пустым
 */
public record Restaurant(String name, boolean cafe, String cuisine, String openingHours, String website,
		GeoPoint point) {
}
