package io.github.lelena1.travelbot.places;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.city.CityDirectory;
import io.github.lelena1.travelbot.city.GeoPoint;
import io.github.lelena1.travelbot.integration.wiki.WikiException;

class NearbyPlacesServiceTest {

	private static final GeoPoint KALININGRAD = new GeoPoint(54.71667, 20.5);
	private static final GeoPoint ZELENOGRADSK = new GeoPoint(54.95, 20.48333);
	private static final GeoPoint SVETLOGORSK = new GeoPoint(54.9394, 20.1565);

	@Test
	void kaliningradDayTripsWithTopSights() {
		CityLocator locator = mock(CityLocator.class);
		SightsService sights = mock(SightsService.class);
		when(locator.locate("Зеленоградск")).thenReturn(Optional.of(ZELENOGRADSK));
		when(locator.locate("Светлогорск (Калининградская область)")).thenReturn(Optional.of(SVETLOGORSK));
		when(locator.locate("Куршская коса")).thenThrow(new WikiException("timeout", null));
		when(sights.topSights(anyString(), any())).thenReturn(List.of(
				sight("A"), sight("B"), sight("C"), sight("D")));

		List<NearbyPlacesService.NearbyPlace> places =
				new NearbyPlacesService(new CityDirectory(), locator, sights).forCity("Калининград", KALININGRAD);

		assertThat(places).extracting(NearbyPlacesService.NearbyPlace::name)
				.as("недоступная Куршская коса пропущена, остальные на месте")
				.containsExactly("Зеленоградск", "Светлогорск");
		assertThat(places.get(0).distanceKm()).isBetween(24, 28);
		assertThat(places.get(0).sights()).hasSize(NearbyPlacesService.SIGHTS_PER_PLACE);
		assertThat(places.get(1).articleUrl()).startsWith("https://ru.wikipedia.org/wiki/");
	}

	@Test
	void cityWithoutDayTrips() {
		assertThat(new NearbyPlacesService(new CityDirectory(), mock(CityLocator.class), mock(SightsService.class))
				.forCity("Новосибирск", KALININGRAD)).isEmpty();
	}

	@Test
	void displayNameDropsClarification() {
		assertThat(NearbyPlacesService.displayName("Светлогорск (Калининградская область)")).isEqualTo("Светлогорск");
		assertThat(NearbyPlacesService.displayName("Куршская коса")).isEqualTo("Куршская коса");
	}

	private static Sight sight(String title) {
		return new Sight(title, null, null, ZELENOGRADSK, "u", 1);
	}
}
