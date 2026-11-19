package io.github.lelena1.travelbot.integration.osm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.github.lelena1.travelbot.city.GeoPoint;

/** Разбор настоящего ответа Overpass для центра Калининграда (src/test/resources/osm). */
class OverpassClientTest {

	private MockRestServiceServer server;
	private OverpassClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new OverpassClient(builder);
	}

	@Test
	void parsesNodesAndBuildings() {
		server.expect(requestTo(OverpassClient.API))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().string(allOf(containsString("restaurant%7Ccafe"), containsString("around%3A1500%2C54.716670%2C20.500000"))))
				.andRespond(withSuccess(new ClassPathResource("osm/food-kaliningrad.json"), MediaType.APPLICATION_JSON));

		List<OsmPlace> places = client.restaurantsAndCafes(new GeoPoint(54.71667, 20.5), 1500);

		assertThat(places).hasSize(40);
		assertThat(places).allSatisfy(place -> {
			assertThat(place.name()).isNotBlank();
			assertThat(place.amenity()).isIn("restaurant", "cafe");
			assertThat(place.point()).isNotNull();
		});
		server.verify();
	}

	@Test
	void serverErrorBecomesOverpassException() {
		server.expect(requestTo(OverpassClient.API)).andRespond(withServerError());

		assertThatThrownBy(() -> client.restaurantsAndCafes(new GeoPoint(54.7, 20.5), 1500))
				.isInstanceOf(OverpassClient.OverpassException.class);
	}
}
