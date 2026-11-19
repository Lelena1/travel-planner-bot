package io.github.lelena1.travelbot.integration.wiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WikidataClientTest {

	@Test
	void parsesRealEntities() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(startsWith(WikidataClient.API)))
				.andExpect(queryParam("action", "wbgetentities"))
				.andExpect(queryParam("props", "sitelinks%7Cclaims"))
				.andRespond(withSuccess(new ClassPathResource("wiki/entities-kaliningrad.json"),
						MediaType.APPLICATION_JSON));

		Map<String, WikidataFacts> facts = new WikidataClient(builder).facts(List.of("Q1829", "Q1829"));

		assertThat(facts).hasSizeGreaterThan(30);
		WikidataFacts kaliningrad = facts.get("Q1829");
		assertThat(kaliningrad.hasPopulation()).isTrue();          // это город, а не достопримечательность
		assertThat(kaliningrad.instanceOf()).isNotEmpty();
		assertThat(kaliningrad.sitelinks()).isGreaterThan(50);    // статьи на десятках языков
		server.verify();
	}

	@Test
	void eventIsRecognizedByDates() {
		WikidataFacts battle = new WikidataFacts(20, java.util.Set.of("Q178561"), java.util.Set.of("P31", "P580", "P582"));
		WikidataFacts museum = new WikidataFacts(15, java.util.Set.of("Q33506"), java.util.Set.of("P31", "P571"));

		assertThat(battle.isEvent()).isTrue();
		assertThat(museum.isEvent()).isFalse();
		assertThat(museum.hasPopulation()).isFalse();
	}
}
