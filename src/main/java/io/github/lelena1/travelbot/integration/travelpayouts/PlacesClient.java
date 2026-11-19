package io.github.lelena1.travelbot.integration.travelpayouts;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Справочник городов Travelpayouts: «Рим» → ROM. Работает без токена.
 *
 * @see <a href="https://support.travelpayouts.com/hc/en-us/articles/360002408712">Autocomplete API</a>
 */
@Component
public class PlacesClient {

	private static final ParameterizedTypeReference<List<Place>> PLACES = new ParameterizedTypeReference<>() {
	};

	private final RestClient http;

	public PlacesClient(RestClient.Builder builder, TravelpayoutsProperties properties) {
		this.http = builder.baseUrl(properties.autocompleteUrl()).build();
	}

	/** Города и страны, подходящие под запрос; самые популярные — первыми. */
	public List<Place> search(String term) {
		try {
			List<Place> places = http.get()
					.uri(uri -> uri.path("/places2")
							.queryParam("term", term)
							.queryParam("locale", "ru")
							.queryParam("types[]", "city")
							.queryParam("types[]", "country")
							.build())
					.retrieve()
					.body(PLACES);
			return places == null ? List.of() : places;
		} catch (RestClientException e) {
			throw new TravelpayoutsException("Не удалось найти город «" + term + "»", e);
		}
	}
}
