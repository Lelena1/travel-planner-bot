package io.github.lelena1.travelbot.integration.wiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.github.lelena1.travelbot.city.GeoPoint;

/** Разбор настоящих ответов Википедии (src/test/resources/wiki). */
class WikipediaClientTest {

	private MockRestServiceServer server;
	private WikipediaClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new WikipediaClient(builder);
	}

	@Test
	void cityCoordinates() {
		server.expect(requestTo(startsWith(WikipediaClient.API)))
				.andExpect(queryParam("prop", "coordinates"))
				.andExpect(queryParam("redirects", "1"))
				.andRespond(json("wiki/coordinates-kaliningrad.json"));

		GeoPoint point = client.coordinates("Калининград").orElseThrow();

		assertThat(point.lat()).isBetween(54.6, 54.8);
		assertThat(point.lon()).isBetween(20.4, 20.6);
	}

	@Test
	void nearbyArticlesWithWikidataIds() {
		server.expect(requestTo(startsWith(WikipediaClient.API)))
				.andExpect(queryParam("generator", "geosearch"))
				.andExpect(queryParam("ggsradius", "10000"))
				.andExpect(queryParam("colimit", "max"))
				.andRespond(json("wiki/nearby-kaliningrad.json"));

		List<WikiArticle> articles = client.nearby(new GeoPoint(54.71, 20.51), 10_000, 50);

		assertThat(articles).hasSizeGreaterThan(30);
		assertThat(articles).allSatisfy(article -> assertThat(article.point()).isNotNull());
		assertThat(articles).filteredOn(article -> article.wikidataId() != null).hasSizeGreaterThan(30);
		assertThat(articles).extracting(WikiArticle::coordinateType).contains("landmark");
	}

	@Test
	void introductions() {
		server.expect(requestTo(startsWith(WikipediaClient.API)))
				.andExpect(queryParam("prop", "extracts"))
				.andRespond(json("wiki/extracts-kaliningrad.json"));

		assertThat(client.introductions(List.of("Кёнигсбергский собор", "Кёнигсбергский замок", "Калининградский зоопарк")))
				.containsKeys("Кёнигсбергский собор", "Кёнигсбергский замок")
				.allSatisfy((title, text) -> assertThat(text).isNotBlank());
	}

	@Test
	void articleUrl() {
		assertThat(WikipediaClient.articleUrl("Кёнигсбергский собор"))
				.isEqualTo("https://ru.wikipedia.org/wiki/%D0%9A%D1%91%D0%BD%D0%B8%D0%B3%D1%81%D0%B1%D0%B5%D1%80%D0%B3%D1%81%D0%BA%D0%B8%D0%B9_%D1%81%D0%BE%D0%B1%D0%BE%D1%80");
	}

	@Test
	void serverErrorBecomesWikiException() {
		server.expect(requestTo(startsWith(WikipediaClient.API))).andRespond(withServerError());

		assertThatThrownBy(() -> client.coordinates("Калининград")).isInstanceOf(WikiException.class);
	}

	private static org.springframework.test.web.client.ResponseCreator json(String resource) {
		return withSuccess(new ClassPathResource(resource), MediaType.APPLICATION_JSON);
	}
}
