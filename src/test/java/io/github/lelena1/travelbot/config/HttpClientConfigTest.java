package io.github.lelena1.travelbot.config;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpClientConfigTest {

	@Test
	void everyRequestIntroducesTheBot() {
		RestClient.Builder builder = RestClient.builder();
		new HttpClientConfig().timeoutsAndUserAgent().customize(builder);
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://example.test/"))
				.andExpect(header("User-Agent", HttpClientConfig.USER_AGENT))
				.andRespond(withSuccess());

		builder.build().get().uri("https://example.test/").retrieve().toBodilessEntity();

		server.verify();
	}
}
