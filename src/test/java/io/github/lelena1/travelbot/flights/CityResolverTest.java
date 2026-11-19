package io.github.lelena1.travelbot.flights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.flights.CityResolver.CountryOnly;
import io.github.lelena1.travelbot.flights.CityResolver.Found;
import io.github.lelena1.travelbot.flights.CityResolver.NotFound;
import io.github.lelena1.travelbot.integration.travelpayouts.Place;
import io.github.lelena1.travelbot.integration.travelpayouts.PlacesClient;

class CityResolverTest {

	private static final Place ROME = new Place("ROM", "Рим", "city", "Италия", null);
	private static final Place MILAN = new Place("MIL", "Милан", "city", "Италия", null);
	private static final Place ITALY = new Place("IT", "Италия", "country", null, null);

	private PlacesClient places;
	private CityResolver resolver;

	@BeforeEach
	void setUp() {
		places = mock(PlacesClient.class);
		resolver = new CityResolver(places);
	}

	@Test
	void singleCity() {
		when(places.search("Рим")).thenReturn(List.of(ROME));

		assertThat(resolver.resolve("Рим")).isEqualTo(new Found(ROME));
	}

	@Test
	void countryAndCityUsesCityFromTheEnd() {
		when(places.search("Рим")).thenReturn(List.of(ROME));

		assertThat(resolver.resolve("Италия, Рим")).isEqualTo(new Found(ROME));
		verify(places, never()).search("Италия");
	}

	@Test
	void cityAndCountryInReverseOrderAlsoWorks() {
		when(places.search("Италия")).thenReturn(List.of(ITALY, MILAN));
		when(places.search("Рим")).thenReturn(List.of(ROME));

		assertThat(resolver.resolve("Рим, Италия")).isEqualTo(new Found(ROME));
	}

	@Test
	void onlyCountryAsksForCity() {
		when(places.search("Италия")).thenReturn(List.of(ITALY, MILAN));

		assertThat(resolver.resolve("Италия")).isEqualTo(new CountryOnly("Италия"));
	}

	@Test
	void unknownPlace() {
		when(places.search("Хогвартс")).thenReturn(List.of());

		assertThat(resolver.resolve("Хогвартс")).isEqualTo(new NotFound("Хогвартс"));
	}
}
