package io.github.lelena1.travelbot.city;

import java.util.Locale;

/** Точка на карте. */
public record GeoPoint(double lat, double lon) {

	/** Расстояние в метрах (формула гаверсинусов — достаточно точно для города). */
	public double distanceTo(GeoPoint other) {
		double earthRadius = 6_371_000;
		double dLat = Math.toRadians(other.lat - lat);
		double dLon = Math.toRadians(other.lon - lon);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return 2 * earthRadius * Math.asin(Math.sqrt(a));
	}

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
