package io.github.lelena1.travelbot.places;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.github.lelena1.travelbot.city.GeoPoint;
import io.github.lelena1.travelbot.integration.wiki.WikiArticle;
import io.github.lelena1.travelbot.integration.wiki.WikidataClient;
import io.github.lelena1.travelbot.integration.wiki.WikidataFacts;
import io.github.lelena1.travelbot.integration.wiki.WikipediaClient;

/** На настоящих ответах Википедии и Викиданных для центра Калининграда. */
class SightsServiceTest {

	@Test
	void kaliningradTopSightsAreRealPlaces() {
		RestClient.Builder wikiBuilder = RestClient.builder();
		MockRestServiceServer wiki = MockRestServiceServer.bindTo(wikiBuilder).build();
		RestClient.Builder dataBuilder = RestClient.builder();
		MockRestServiceServer data = MockRestServiceServer.bindTo(dataBuilder).build();
		wiki.expect(requestTo(startsWith(WikipediaClient.API))).andExpect(queryParam("generator", "geosearch"))
				.andRespond(json("wiki/nearby-kaliningrad.json"));
		data.expect(requestTo(startsWith("https://www.wikidata.org")))
				.andRespond(json("wiki/entities-kaliningrad.json"));
		wiki.expect(requestTo(startsWith(WikipediaClient.API))).andExpect(queryParam("prop", "extracts"))
				.andRespond(json("wiki/extracts-kaliningrad.json"));
		SightsService service = new SightsService(new WikipediaClient(wikiBuilder), new WikidataClient(dataBuilder));

		List<Sight> sights = service.topSights("Калининград", new GeoPoint(54.71667, 20.5));

		assertThat(sights).isNotEmpty().hasSizeLessThanOrEqualTo(SightsService.TOP);
		assertThat(sights).extracting(Sight::title)
				// в образце 50 ближайших к центру статей (бот смотрит 200), кафедральный собор чуть дальше
				.contains("Кёнигсбергский замок", "Калининградский зоопарк")
				.doesNotContain("Калининград", "Кёнигсберг", "Калининградская область", "Центральный район (Калининград)");
		assertThat(sights).extracting(Sight::fame).isSortedAccordingTo((a, b) -> b - a);
		assertThat(sights).filteredOn(sight -> sight.title().equals("Кёнигсбергский замок"))
				.singleElement().satisfies(sight -> {
					assertThat(sight.intro()).isNotBlank();
					assertThat(sight.articleUrl()).startsWith("https://ru.wikipedia.org/wiki/");
				});
		wiki.verify();
		data.verify();
	}

	@Test
	void citiesStreetsAndFactoriesAreNotSights() {
		GeoPoint p = new GeoPoint(0, 0);

		assertThat(SightsService.looksLikeSight(new WikiArticle("Калининград", "город", p, "city", "Q1"), "Калининград")).isFalse();
		assertThat(SightsService.looksLikeSight(new WikiArticle("Ленинский проспект", null, p, null, "Q2"), "Калининград")).isFalse();
		assertThat(SightsService.looksLikeSight(new WikiArticle("Янтарь", "судостроительный завод", p, null, "Q3"), "Калининград")).isFalse();
		assertThat(SightsService.looksLikeSight(new WikiArticle("Кёнигсбергский собор", "собор", p, "landmark", "Q4"), "Калининград")).isTrue();
		// так выглядели «достопримечательности» Зеленоградска с радиусом 10 км
		assertThat(SightsService.looksLikeSight(new WikiArticle("Храброво (аэропорт)", null, p, null, "Q5"), "Зеленоградск")).isFalse();
		assertThat(SightsService.looksLikeSight(new WikiArticle("Исторический центр Санкт-Петербурга и связанные с ним группы памятников",
				null, p, null, "Q7"), "Санкт-Петербург")).isFalse();
		assertThat(SightsService.looksLikeSight(new WikiArticle("Тростянка (река, впадает в Балтийское море)", null, p, null, "Q6"), "Зеленоградск")).isFalse();
	}

	@Test
	void eventsRegionsAndSettlementsAreNotPlaces() {
		assertThat(SightsService.isPlace(new WikidataFacts(27, Set.of("Q178561"), Set.of("P31", "P580", "P582")))).isFalse();
		assertThat(SightsService.isPlace(new WikidataFacts(34, Set.of("Q1620908"), Set.of("P31")))).isFalse();
		assertThat(SightsService.isPlace(new WikidataFacts(54, Set.of("Q515"), Set.of("P31", "P1082")))).isFalse();
		assertThat(SightsService.isPlace(new WikidataFacts(32, Set.of("Q2977"), Set.of("P31", "P571")))).isTrue();
		assertThat(SightsService.isPlace(new WikidataFacts(12, Set.of("Q4022"), Set.of("P31")))).as("река").isFalse();
	}

	private static org.springframework.test.web.client.ResponseCreator json(String resource) {
		return withSuccess(new ClassPathResource(resource), MediaType.APPLICATION_JSON);
	}
}
