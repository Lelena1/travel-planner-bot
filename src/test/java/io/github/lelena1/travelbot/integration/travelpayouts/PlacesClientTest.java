package io.github.lelena1.travelbot.integration.travelpayouts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Разбор настоящих ответов справочника (сохранены в src/test/resources/travelpayouts). */
class PlacesClientTest {

	private MockRestServiceServer server;
	private PlacesClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new PlacesClient(builder, TestProperties.TRAVELPAYOUTS);
	}

	@Test
	void findsRomeWithDeclensions() {
		server.expect(requestTo(startsWith("https://autocomplete.test/places2")))
				.andExpect(method(HttpMethod.GET))
				.andExpect(queryParam("locale", "ru"))
				.andRespond(withSuccess(new ClassPathResource("travelpayouts/places-rome.json"),
						MediaType.APPLICATION_JSON));

		Place rome = client.search("Рим").get(0);

		assertThat(rome.code()).isEqualTo("ROM");
		assertThat(rome.isCity()).isTrue();
		assertThat(rome.countryName()).isEqualTo("Италия");
		assertThat(rome.toDirection()).isEqualTo("в Рим");
		assertThat(rome.genitive()).isEqualTo("Рима");
		server.verify();
	}

	@Test
	void countryComesFirstForCountryName() {
		server.expect(requestTo(startsWith("https://autocomplete.test/places2")))
				.andRespond(withSuccess(new ClassPathResource("travelpayouts/places-italy.json"),
						MediaType.APPLICATION_JSON));

		assertThat(client.search("Италия").get(0).isCountry()).isTrue();
	}

	@Test
	void serverErrorBecomesTravelpayoutsException() {
		server.expect(requestTo(startsWith("https://autocomplete.test/places2"))).andRespond(withServerError());

		assertThatThrownBy(() -> client.search("Рим")).isInstanceOf(TravelpayoutsException.class);
	}
}
