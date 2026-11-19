package io.github.lelena1.travelbot.integration.wiki;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.lelena1.travelbot.city.GeoPoint;

/**
 * Русская Википедия (MediaWiki API): координаты города, статьи рядом с точкой, короткие описания.
 * Бесплатно и без ключа.
 *
 * @see <a href="https://ru.wikipedia.org/w/api.php">MediaWiki API</a>
 */
@Component
public class WikipediaClient {

	static final String API = "https://ru.wikipedia.org/w/api.php";
	/** Не больше 20 описаний за один запрос — ограничение API. */
	static final int MAX_EXTRACTS = 20;

	private final RestClient http;

	public WikipediaClient(RestClient.Builder builder) {
		this.http = builder.baseUrl(API).build();
	}

	/** Координаты статьи («Калининград»); перенаправления («Питер») учитываются. */
	public Optional<GeoPoint> coordinates(String title) {
		Response response = get(Map.of("prop", "coordinates", "titles", title, "redirects", "1"));
		return pages(response).stream()
				.filter(page -> page.coordinates() != null && !page.coordinates().isEmpty())
				.map(page -> page.coordinates().get(0).point())
				.findFirst();
	}

	/** Статьи с координатами в радиусе {@code radiusMeters} (не больше 10 км) от точки. */
	public List<WikiArticle> nearby(GeoPoint point, int radiusMeters, int limit) {
		Response response = get(Map.of(
				"generator", "geosearch",
				"ggscoord", point.lat() + "|" + point.lon(),
				"ggsradius", String.valueOf(radiusMeters),
				"ggslimit", String.valueOf(limit),
				"prop", "coordinates|pageprops|description",
				"coprop", "type",
				// без этого координаты приходят только у первых 10 статей
				"colimit", "max",
				"ppprop", "wikibase_item"));
		return pages(response).stream()
				.filter(page -> page.coordinates() != null && !page.coordinates().isEmpty())
				.map(page -> new WikiArticle(page.title(), page.description(), page.coordinates().get(0).point(),
						page.coordinates().get(0).type(),
						page.pageprops() == null ? null : page.pageprops().wikibaseItem()))
				.toList();
	}

	/** Первые предложения статей: название → текст. */
	public Map<String, String> introductions(List<String> titles) {
		Map<String, String> result = new HashMap<>();
		for (int i = 0; i < titles.size(); i += MAX_EXTRACTS) {
			List<String> batch = titles.subList(i, Math.min(titles.size(), i + MAX_EXTRACTS));
			Response response = get(Map.of("prop", "extracts", "exintro", "1", "explaintext", "1",
					"exsentences", "2", "exlimit", String.valueOf(MAX_EXTRACTS), "titles", String.join("|", batch)));
			pages(response).stream()
					.filter(page -> page.extract() != null && !page.extract().isBlank())
					.forEach(page -> result.put(page.title(), page.extract().strip()));
		}
		return result;
	}

	/** Адрес статьи на сайте. */
	public static String articleUrl(String title) {
		return "https://ru.wikipedia.org/wiki/" + java.net.URLEncoder
				.encode(title.replace(' ', '_'), java.nio.charset.StandardCharsets.UTF_8);
	}

	private Response get(Map<String, String> params) {
		try {
			return http.get()
					.uri(uri -> {
						uri.queryParam("action", "query").queryParam("format", "json").queryParam("formatversion", "2");
						params.forEach(uri::queryParam);
						return uri.build();
					})
					.retrieve()
					.body(Response.class);
		} catch (RestClientException e) {
			throw new WikiException("Википедия не ответила", e);
		}
	}

	private static List<Page> pages(Response response) {
		return response == null || response.query() == null || response.query().pages() == null
				? List.of()
				: response.query().pages();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Response(Query query) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Query(List<Page> pages) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Page(String title, String description, List<Coordinate> coordinates, PageProps pageprops, String extract) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Coordinate(double lat, double lon, String type) {
		GeoPoint point() {
			return new GeoPoint(lat, lon);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record PageProps(@JsonProperty("wikibase_item") String wikibaseItem) {
	}
}
