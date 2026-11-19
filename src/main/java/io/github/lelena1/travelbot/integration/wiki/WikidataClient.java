package io.github.lelena1.travelbot.integration.wiki;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Викиданные: насколько известен объект (в скольких языковых Википедиях есть статья)
 * и что он такое (музей, событие, город…). Обычный API, а не SPARQL — у него нет жёстких лимитов.
 *
 * @see <a href="https://www.wikidata.org/w/api.php?action=help&amp;modules=wbgetentities">wbgetentities</a>
 */
@Component
public class WikidataClient {

	static final String API = "https://www.wikidata.org/w/api.php";
	/** Не больше 50 объектов за запрос — ограничение API. */
	static final int MAX_IDS = 50;

	private final RestClient http;

	public WikidataClient(RestClient.Builder builder) {
		this.http = builder.baseUrl(API).build();
	}

	public Map<String, WikidataFacts> facts(List<String> ids) {
		Map<String, WikidataFacts> result = new HashMap<>();
		List<String> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
		for (int i = 0; i < distinct.size(); i += MAX_IDS) {
			List<String> batch = distinct.subList(i, Math.min(distinct.size(), i + MAX_IDS));
			Response response;
			try {
				response = http.get()
						.uri(uri -> uri.queryParam("action", "wbgetentities")
								.queryParam("ids", String.join("|", batch))
								.queryParam("props", "sitelinks|claims")
								.queryParam("format", "json")
								.build())
						.retrieve()
						.body(Response.class);
			} catch (RestClientException e) {
				throw new WikiException("Викиданные не ответили", e);
			}
			if (response != null && response.entities() != null) {
				response.entities().forEach((id, entity) -> result.put(id, entity.toFacts()));
			}
		}
		return result;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Response(Map<String, Entity> entities) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Entity(Map<String, Object> sitelinks, Map<String, List<Claim>> claims) {

		WikidataFacts toFacts() {
			Map<String, List<Claim>> all = claims == null ? Map.of() : claims;
			Set<String> instanceOf = all.getOrDefault("P31", List.of()).stream()
					.map(Claim::itemId)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
			return new WikidataFacts(sitelinks == null ? 0 : sitelinks.size(), instanceOf, all.keySet());
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Claim(Snak mainsnak) {

		String itemId() {
			if (mainsnak == null || mainsnak.datavalue() == null
					|| !(mainsnak.datavalue().value() instanceof Map<?, ?> value)) {
				return null;
			}
			return (String) value.get("id");
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Snak(DataValue datavalue) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record DataValue(Object value) {
	}
}
