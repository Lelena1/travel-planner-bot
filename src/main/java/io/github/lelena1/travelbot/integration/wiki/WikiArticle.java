package io.github.lelena1.travelbot.integration.wiki;

import io.github.lelena1.travelbot.city.GeoPoint;

/**
 * Статья Википедии с координатами.
 *
 * @param coordinateType тип объекта из координат статьи: landmark, city, adm1st, event… (может быть пустым)
 * @param wikidataId     id в Викиданных, например Q1829
 */
public record WikiArticle(String title, String description, GeoPoint point, String coordinateType,
		String wikidataId) {
}
