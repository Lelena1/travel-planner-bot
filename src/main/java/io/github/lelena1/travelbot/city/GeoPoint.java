package io.github.lelena1.travelbot.city;

import java.util.Locale;

/** Точка на карте. */
public record GeoPoint(double lat, double lon) {

	/** Ссылка на точку в Яндекс Картах. */
	public String yandexMapsUrl() {
		return String.format(Locale.ROOT, "https://yandex.ru/maps/?pt=%.6f,%.6f&z=16&l=map", lon, lat);
	}

	/** Поиск на Яндекс Картах рядом с точкой: «рестораны», «Тётка Фишер». */
	public String yandexMapsSearchUrl(String text) {
		return String.format(Locale.ROOT, "https://yandex.ru/maps/?text=%s&ll=%.6f,%.6f&z=15",
				java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"), lon, lat);
	}
}
