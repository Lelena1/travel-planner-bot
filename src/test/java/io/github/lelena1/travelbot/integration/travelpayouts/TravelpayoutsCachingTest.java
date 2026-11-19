package io.github.lelena1.travelbot.integration.travelpayouts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.github.lelena1.travelbot.config.CacheConfig;

/** Повторный одинаковый запрос берётся из кеша и не уходит в API. */
@SpringJUnitConfig(classes = { CacheConfig.class, TravelpayoutsCachingTest.Clients.class })
class TravelpayoutsCachingTest {

	@Configuration(proxyBeanMethods = false)
	static class Clients {

		@Bean
		RestClient.Builder restClientBuilder() {
			return RestClient.builder();
		}

		@Bean
		MockRestServiceServer server(RestClient.Builder builder) {
			return MockRestServiceServer.bindTo(builder).build();
		}

		@Bean
		PlacesClient placesClient(RestClient.Builder builder, MockRestServiceServer server) {
			return new PlacesClient(builder, TestProperties.TRAVELPAYOUTS);
		}

		@Bean
		AviasalesClient aviasalesClient(RestClient.Builder builder, MockRestServiceServer server) {
			return new AviasalesClient(builder, TestProperties.TRAVELPAYOUTS);
		}
	}

	@Autowired
	private MockRestServiceServer server;
	@Autowired
	private PlacesClient places;
	@Autowired
	private AviasalesClient aviasales;
	@Autowired
	private CacheManager caches;

	@BeforeEach
	void reset() {
		server.reset();
		caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
	}

	@Test
	void sameCityIsRequestedOnce() {
		server.expect(once(), requestTo(startsWith("https://autocomplete.test/places2")))
				.andRespond(withSuccess(new ClassPathResource("travelpayouts/places-rome.json"),
						MediaType.APPLICATION_JSON));

		places.search("Рим");
		places.search("Рим");

		server.verify();
	}

	@Test
	void samePriceQueryIsRequestedOnceButDifferentQueryGoesToApi() {
		server.expect(once(), requestTo(startsWith("https://api.test/aviasales/v3/prices_for_dates")))
				.andExpect(queryParam("destination", "ROM"))
				.andRespond(withSuccess(new ClassPathResource("travelpayouts/prices-mow-rom.json"),
						MediaType.APPLICATION_JSON));
		server.expect(once(), requestTo(startsWith("https://api.test/aviasales/v3/prices_for_dates")))
				.andExpect(queryParam("destination", "IST"))
				.andRespond(withSuccess("{\"success\":true,\"data\":[]}", MediaType.APPLICATION_JSON));

		FlightQuery rome = FlightQuery.anyDayIn("MOW", "ROM", YearMonth.of(2027, 5));
		assertThat(aviasales.cheapest(rome, 3)).hasSize(3);
		assertThat(aviasales.cheapest(rome, 3)).hasSize(3);
		assertThat(aviasales.cheapest(FlightQuery.anyDayIn("MOW", "IST", YearMonth.of(2027, 5)), 3)).isEmpty();

		server.verify();
	}
}
