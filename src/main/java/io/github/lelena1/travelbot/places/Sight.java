package io.github.lelena1.travelbot.places;

import io.github.lelena1.travelbot.city.GeoPoint;

/**
 * Достопримечательность.
 *
 * @param description короткое описание из Википедии («собор в Калининграде»); может быть пустым
 * @param intro       первые предложения статьи; может быть пустым
 * @param fame        в скольких Википедиях есть статья — чем больше, тем известнее место
 */
public record Sight(String title, String description, String intro, GeoPoint point, String articleUrl, int fame) {
}
