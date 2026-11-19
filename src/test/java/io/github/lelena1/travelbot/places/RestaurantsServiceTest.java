package io.github.lelena1.travelbot.places;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.city.GeoPoint;
import io.github.lelena1.travelbot.integration.osm.OsmPlace;
import io.github.lelena1.travelbot.integration.osm.OverpassClient;

class RestaurantsServiceTest {

	private static final GeoPoint P = new GeoPoint(54.7, 20.5);

	@Test
	void placesWithMoreInfoComeFirst() {
		OverpassClient overpass = mock(OverpassClient.class);
		when(overpass.restaurantsAndCafes(any(), anyInt())).thenReturn(List.of(
				place("Безымянная шаурма", "cafe", Map.of()),
				place("Тётка Фишер", "restaurant", Map.of("cuisine", "german", "opening_hours", "10:00-24:00",
						"website", "http://tetkafisher39.ru/")),
				place("Сачмэли", "restaurant", Map.of("cuisine", "georgian;grill", "opening_hours", "11:00-24:00"))));

		List<Restaurant> top = new RestaurantsService(overpass).top("Калининград", P);

		assertThat(top).extracting(Restaurant::name).containsExactly("Тётка Фишер", "Сачмэли", "Безымянная шаурма");
		assertThat(top.get(0).cuisine()).isEqualTo("немецкая");
		assertThat(top.get(0).website()).isEqualTo("http://tetkafisher39.ru/");
		assertThat(top.get(1).cuisine()).isEqualTo("грузинская, гриль");
		assertThat(top.get(2).cafe()).isTrue();
	}

	@Test
	void cuisineTranslation() {
		assertThat(RestaurantsService.cuisine("international;russian")).isEqualTo("интернациональная, русская");
		assertThat(RestaurantsService.cuisine("something_unknown")).isNull();
		assertThat(RestaurantsService.cuisine(null)).isNull();
	}

	private static OsmPlace place(String name, String amenity, Map<String, String> tags) {
		Map<String, String> all = new java.util.HashMap<>(tags);
		all.put("name", name);
		all.put("amenity", amenity);
		return new OsmPlace(name, amenity, P, all);
	}

	@Test
	void equallyDescribedPlacesCloserToCentreComeFirst() {
		OverpassClient overpass = mock(OverpassClient.class);
		GeoPoint far = new GeoPoint(54.73, 20.5);
		GeoPoint near = new GeoPoint(54.701, 20.5);
		when(overpass.restaurantsAndCafes(any(), anyInt())).thenReturn(List.of(
				new OsmPlace("Amedeo", "restaurant", far, Map.of("cuisine", "italian")),
				new OsmPlace("Zucca", "restaurant", near, Map.of("cuisine", "italian"))));

		assertThat(new RestaurantsService(overpass).top("Калининград", P)).extracting(Restaurant::name)
				.containsExactly("Zucca", "Amedeo");
	}

	@Test
	void openingHoursInRussian() {
		assertThat(RestaurantsService.openingHours("Mo-Su 12:00-23:00")).isEqualTo("Пн-Вс 12:00-23:00");
		assertThat(RestaurantsService.openingHours("Tu-Sa 12:45-22:45; Su off")).isEqualTo("Вт-Сб 12:45-22:45; Вс выходной");
		assertThat(RestaurantsService.openingHours("24/7")).isEqualTo("круглосуточно");
		assertThat(RestaurantsService.openingHours(null)).isNull();
	}
}
