package io.github.lelena1.travelbot.integration.travelpayouts;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.github.lelena1.travelbot.config.CacheConfig;

/**
 * Цены на авиабилеты из Aviasales Data API (метод prices_for_dates).
 *
 * @see <a href="https://support.travelpayouts.com/hc/en-us/articles/203956163">Aviasales Data API</a>
 */
@Component
public class AviasalesClient {

	private final RestClient http;
	private final TravelpayoutsProperties properties;

	public AviasalesClient(RestClient.Builder builder, TravelpayoutsProperties properties) {
		this.properties = properties;
		this.http = builder.baseUrl(properties.apiUrl()).build();
	}

	/** Самые дешёвые варианты, по возрастанию цены. Кешируется на 30 минут. */
	@Cacheable(CacheConfig.FLIGHT_PRICES)
	public List<PriceOffer> cheapest(FlightQuery query, int limit) {
		if (properties.token() == null || properties.token().isBlank()) {
			throw new TravelpayoutsException("Не задан токен Travelpayouts (TRAVELPAYOUTS_TOKEN)");
		}
		PricesResponse response;
		try {
			response = http.get()
					.uri(uri -> {
						uri.path("/aviasales/v3/prices_for_dates")
								.queryParam("origin", query.origin())
								.queryParam("destination", query.destination())
								.queryParam("departure_at", query.departureAt())
								.queryParam("one_way", query.oneWay())
								.queryParam("currency", "rub")
								.queryParam("sorting", "price")
								.queryParam("limit", limit);
						if (query.returnAt() != null) {
							uri.queryParam("return_at", query.returnAt());
						}
						return uri.build();
					})
					.header("X-Access-Token", properties.token())
					.retrieve()
					// на 400 API присылает понятное описание ошибки в теле — разберём его ниже
					.onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
						if (resp.getStatusCode().value() == 401) {
							throw new TravelpayoutsException("Токен Travelpayouts не подходит (401)");
						}
					})
					.body(PricesResponse.class);
		} catch (RestClientException e) {
			throw new TravelpayoutsException("Aviasales не ответил", e);
		}
		if (response == null || !response.success()) {
			String error = response == null ? "пустой ответ" : response.error();
			throw new TravelpayoutsException("Aviasales вернул ошибку: " + error);
		}
		return response.data() == null ? List.of() : response.data();
	}

	/** Полный адрес страницы Aviasales для найденного варианта. */
	public String offerUrl(PriceOffer offer) {
		return properties.aviasalesUrl() + offer.link();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record PricesResponse(boolean success, List<PriceOffer> data, String error) {
	}
}
