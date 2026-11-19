package io.github.lelena1.travelbot.integration.travelpayouts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AviasalesClientTest {

	private static final String PRICES_URL = "https://api.test/aviasales/v3/prices_for_dates";

	private MockRestServiceServer server;
	private AviasalesClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new AviasalesClient(builder, TestProperties.TRAVELPAYOUTS);
	}

	@Test
	void parsesRealRoundTripResponse() {
		server.expect(requestTo(startsWith(PRICES_URL)))
				.andExpect(header("X-Access-Token", "test-token"))
				.andExpect(queryParam("origin", "MOW"))
				.andExpect(queryParam("destination", "ROM"))
				.andExpect(queryParam("departure_at", "2027-05-12"))
				.andExpect(queryParam("return_at", "2027-05-18"))
				.andExpect(queryParam("one_way", "false"))
				.andExpect(queryParam("sorting", "price"))
				.andRespond(withSuccess(new ClassPathResource("travelpayouts/prices-mow-rom.json"),
						MediaType.APPLICATION_JSON));

		List<PriceOffer> offers = client.cheapest(
				FlightQuery.roundTrip("MOW", "ROM", LocalDate.of(2027, 5, 12), LocalDate.of(2027, 5, 18)), 3);

		assertThat(offers).hasSize(3);
		PriceOffer first = offers.get(0);
		assertThat(first.price()).isEqualTo(47948);
		assertThat(first.originAirport()).isEqualTo("VKO");
		assertThat(first.destinationAirport()).isEqualTo("FCO");
		assertThat(first.departureAt()).isEqualTo(OffsetDateTime.parse("2027-05-12T20:50:00+03:00"));
		assertThat(first.isRoundTrip()).isTrue();
		assertThat(first.isDirect()).isFalse();
		assertThat(offers).extracting(PriceOffer::price).isSorted();
		assertThat(client.offerUrl(first)).startsWith("https://www.aviasales.ru/search/MOW1205ROM18051");
		server.verify();
	}

	@Test
	void oneWayMonthSearchHasNoReturnDate() {
		server.expect(requestTo(startsWith(PRICES_URL)))
				.andExpect(queryParam("departure_at", "2027-05"))
				.andExpect(queryParam("one_way", "true"))
				.andRespond(withSuccess("{\"success\":true,\"data\":[],\"currency\":\"rub\"}",
						MediaType.APPLICATION_JSON));

		assertThat(client.cheapest(FlightQuery.anyDayIn("MOW", "ROM", YearMonth.of(2027, 5)), 5)).isEmpty();
	}

	@Test
	void badRequestShowsApiError() {
		server.expect(requestTo(startsWith(PRICES_URL))).andRespond(withBadRequest()
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"error\":\"bad request: unknown location code `XXX`\",\"data\":null,\"status\":400,\"success\":false}"));

		assertThatThrownBy(() -> client.cheapest(FlightQuery.anyDayIn("MOW", "XXX", YearMonth.of(2027, 5)), 5))
				.isInstanceOf(TravelpayoutsException.class)
				.hasMessageContaining("unknown location code");
	}

	@Test
	void wrongTokenIsReportedClearly() {
		server.expect(requestTo(startsWith(PRICES_URL))).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertThatThrownBy(() -> client.cheapest(FlightQuery.anyDayIn("MOW", "ROM", YearMonth.of(2027, 5)), 5))
				.isInstanceOf(TravelpayoutsException.class)
				.hasMessageContaining("Токен");
	}

	@Test
	void missingTokenFailsWithoutCallingApi() {
		AviasalesClient noToken = new AviasalesClient(RestClient.builder(),
				new TravelpayoutsProperties("", "https://api.test", "https://autocomplete.test", "https://www.aviasales.ru"));

		assertThatThrownBy(() -> noToken.cheapest(FlightQuery.anyDayIn("MOW", "ROM", YearMonth.of(2027, 5)), 5))
				.hasMessageContaining("TRAVELPAYOUTS_TOKEN");
	}
}
