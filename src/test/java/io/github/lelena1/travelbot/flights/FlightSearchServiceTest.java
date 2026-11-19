package io.github.lelena1.travelbot.flights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.flights.FlightSearchService.Found;
import io.github.lelena1.travelbot.flights.FlightSearchService.Problem;
import io.github.lelena1.travelbot.integration.travelpayouts.AviasalesClient;
import io.github.lelena1.travelbot.integration.travelpayouts.FlightQuery;
import io.github.lelena1.travelbot.integration.travelpayouts.Place;
import io.github.lelena1.travelbot.integration.travelpayouts.PriceOffer;
import io.github.lelena1.travelbot.integration.travelpayouts.TestProperties;
import io.github.lelena1.travelbot.integration.travelpayouts.TravelpayoutsException;
import io.github.lelena1.travelbot.trip.Trip;

class FlightSearchServiceTest {

	static final Place MOSCOW = new Place("MOW", "Москва", "city", "Россия", null);
	static final Place ROME = new Place("ROM", "Рим", "city", "Италия", null);
	static final LocalDate THERE = LocalDate.of(2027, 5, 12);
	static final LocalDate BACK = LocalDate.of(2027, 5, 18);

	private CityResolver cities;
	private AviasalesClient aviasales;
	private FlightSearchService service;
	private Trip trip;

	@BeforeEach
	void setUp() {
		cities = mock(CityResolver.class);
		aviasales = mock(AviasalesClient.class);
		service = new FlightSearchService(cities, aviasales, TestProperties.TRAVELPAYOUTS);
		trip = trip("Москва", "Италия, Рим");
		when(cities.resolve("Москва")).thenReturn(new CityResolver.Found(MOSCOW));
		when(cities.resolve("Италия, Рим")).thenReturn(new CityResolver.Found(ROME));
	}

	@Test
	void findsRoundTripOnTripDates() {
		PriceOffer offer = offer(47948);
		when(aviasales.cheapest(FlightQuery.roundTrip("MOW", "ROM", THERE, BACK), 5)).thenReturn(List.of(offer));

		Found found = (Found) service.search(trip);

		assertThat(found.exactDates()).isTrue();
		assertThat(found.offers()).containsExactly(offer);
		assertThat(found.searchUrl()).isEqualTo("https://www.aviasales.ru/search/MOW1205ROM180521");
	}

	@Test
	void fallsBackToCheapestDaysInMonth() {
		PriceOffer offer = offer(21000);
		when(aviasales.cheapest(FlightQuery.roundTrip("MOW", "ROM", THERE, BACK), 5)).thenReturn(List.of());
		when(aviasales.cheapest(FlightQuery.anyDayIn("MOW", "ROM", YearMonth.of(2027, 5)), 5))
				.thenReturn(List.of(offer));

		Found found = (Found) service.search(trip);

		assertThat(found.exactDates()).isFalse();
		assertThat(found.offers()).containsExactly(offer);
	}

	@Test
	void countryInsteadOfCityAsksForCity() {
		trip = trip("Москва", "Италия");
		when(cities.resolve("Италия")).thenReturn(new CityResolver.CountryOnly("Италия"));

		assertThat(((Problem) service.search(trip)).message()).contains("это страна").contains("Италия, Рим");
	}

	@Test
	void unknownCity() {
		trip = trip("Москва", "Хогвартс");
		when(cities.resolve("Хогвартс")).thenReturn(new CityResolver.NotFound("Хогвартс"));

		assertThat(((Problem) service.search(trip)).message()).contains("Не нашла аэропорт для «Хогвартс»");
	}

	@Test
	void sameCityIsRejected() {
		trip = trip("Москва", "Москва");

		assertThat(((Problem) service.search(trip)).message()).contains("совпадают");
	}

	@Test
	void serviceUnavailable() {
		when(aviasales.cheapest(any(), anyInt())).thenThrow(new TravelpayoutsException("timeout"));

		assertThat(((Problem) service.search(trip)).message()).contains("не отвечает");
	}

	static Trip trip(String origin, String destination) {
		Trip trip = Trip.draftFor(1);
		trip.setOrigin(origin);
		trip.setDestination(destination);
		trip.setDates(THERE, BACK);
		trip.setAdults(2);
		trip.setChildren(1);
		trip.setChildAges(List.of(5));
		return trip;
	}

	static PriceOffer offer(int price) {
		return new PriceOffer("MOW", "ROM", "VKO", "FCO", OffsetDateTime.parse("2027-05-12T20:50:00+03:00"),
				OffsetDateTime.parse("2027-05-18T11:55:00+02:00"), "UT", "785", price, 1, 0, 475, 230,
				"/search/MOW1205ROM18051?t=abc");
	}
}
