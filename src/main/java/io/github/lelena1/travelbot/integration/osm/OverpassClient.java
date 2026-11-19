package io.github.lelena1.travelbot.integration.osm;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.github.lelena1.travelbot.city.GeoPoint;

/**
 * Overpass API — запросы к данным OpenStreetMap. Бесплатно и без ключа;
 * публичный сервер просит не нагружать его, поэтому ответы кешируются на неделю.
 *
 * @see <a href="https://wiki.openstreetmap.org/wiki/Overpass_API">Overpass API</a>
 */
@Component
public class OverpassClient {

	private static final Logger log = LoggerFactory.getLogger(OverpassClient.class);

	public static final String API = "https://overpass-api.de/api/interpreter";
	/** Запасной сервер: публичные серверы Overpass иногда перегружены или недоступны. */
	public static final String MIRROR = "https://overpass.private.coffee/api/interpreter";
	static final List<String> SERVERS = List.of(API, MIRROR);
	static final int MAX_RESULTS = 300;

	private final RestClient http;

	public OverpassClient(RestClient.Builder builder) {
		this.http = builder.build();
	}

	/** Рестораны и кафе с названием в радиусе {@code radiusMeters} от точки. */
	public List<OsmPlace> restaurantsAndCafes(GeoPoint center, int radiusMeters) {
		String around = String.format(Locale.ROOT, "around:%d,%.6f,%.6f", radiusMeters, center.lat(), center.lon());
		String query = """
				[out:json][timeout:50];
				(node["amenity"~"^(restaurant|cafe)$"]["name"](%1$s);
				 way["amenity"~"^(restaurant|cafe)$"]["name"](%1$s););
				out center tags %2$d;""".formatted(around, MAX_RESULTS);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("data", query);
		RestClientException last = null;
		for (String server : SERVERS) {
			try {
				Response response = http.post()
						.uri(server)
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.body(form)
						.retrieve()
						.body(Response.class);
				if (response == null || response.elements() == null) {
					return List.of();
				}
				return response.elements().stream().map(Element::toPlace).filter(Objects::nonNull).toList();
			} catch (RestClientException e) {
				log.warn("Overpass {} не ответил: {}", server, e.getMessage());
				last = e;
			}
		}
		throw new OverpassException("Ни один сервер OpenStreetMap (Overpass) не ответил", last);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Response(List<Element> elements) {
	}

	/** Точка (node) с lat/lon или здание (way) с центром center. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record Element(Double lat, Double lon, Center center, Map<String, String> tags) {

		OsmPlace toPlace() {
			if (tags == null || tags.get("name") == null) {
				return null;
			}
			GeoPoint point = lat != null ? new GeoPoint(lat, lon)
					: center != null ? new GeoPoint(center.lat(), center.lon()) : null;
			return point == null ? null : new OsmPlace(tags.get("name"), tags.get("amenity"), point, tags);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Center(double lat, double lon) {
	}

	/** Сервер OpenStreetMap не ответил или ответил ошибкой. */
	public static class OverpassException extends RuntimeException {
		public OverpassException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
