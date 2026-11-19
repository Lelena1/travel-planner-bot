package io.github.lelena1.travelbot.lodging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.city.KnownCity;
import io.github.lelena1.travelbot.lodging.LodgingLink.Filter;
import io.github.lelena1.travelbot.trip.Budget;
import io.github.lelena1.travelbot.trip.Pets;

class LodgingSitesTest {

	private static final KnownCity KAZAN = new KnownCity("Казань", List.of(), "russia/kazan", "kazan", "kazan", true, null);
	private static final KnownCity ROME = new KnownCity("Рим", List.of(), "italy/rome", null, null, false, null);
	private static final LocalDate IN = LocalDate.of(2027, 5, 12);
	private static final LocalDate OUT = LocalDate.of(2027, 5, 18);

	@Test
	void ostrovokWithChildrenAges() {
		LodgingLink link = new OstrovokSite().link(request(KAZAN, 2, List.of(5, 8), Pets.DOG)).orElseThrow();

		assertThat(link.url()).isEqualTo(
				"https://ostrovok.ru/hotel/russia/kazan/?search=yes&dates=12.05.2027-18.05.2027&guests=2and5.8");
		assertThat(link.applied()).containsExactlyInAnyOrder(Filter.DATES, Filter.GUESTS);
	}

	@Test
	void ostrovokAdultsOnly() {
		assertThat(new OstrovokSite().link(request(ROME, 2, List.of(), Pets.NONE)).orElseThrow().url())
				.endsWith("/hotel/italy/rome/?search=yes&dates=12.05.2027-18.05.2027&guests=2");
	}

	@Test
	void ostrovokNeedsKnownCity() {
		assertThat(new OstrovokSite().link(unknown("Хогвартс"))).isEmpty();
	}

	@Test
	void sutochnoWithChildrenAndPets() {
		LodgingLink link = new SutochnoSite().link(request(KAZAN, 2, List.of(5, 8), Pets.DOG)).orElseThrow();

		assertThat(link.url()).isEqualTo("https://sutochno.ru/front/searchapp/search"
				+ "?term=%D0%9A%D0%B0%D0%B7%D0%B0%D0%BD%D1%8C&occupied=2027-05-12;2027-05-18"
				+ "&guests_adults=2&guests_childrens=5,8&pets=1");
		assertThat(link.applied()).containsExactlyInAnyOrder(Filter.DATES, Filter.GUESTS, Filter.PETS);
	}

	@Test
	void sutochnoWorksForCityOutsideDirectory() {
		LodgingLink link = new SutochnoSite().link(unknown("Великие Луки")).orElseThrow();

		assertThat(link.url()).contains("term=%D0%92%D0%B5%D0%BB%D0%B8%D0%BA%D0%B8%D0%B5%20%D0%9B%D1%83%D0%BA%D0%B8")
				.doesNotContain("pets");
	}

	@Test
	void sutochnoSkipsCitiesItDoesNotCover() {
		assertThat(new SutochnoSite().link(request(ROME, 2, List.of(), Pets.NONE))).isEmpty();
	}

	@Test
	void avitoAndCianOnlyForRussianCities() {
		assertThat(new AvitoSite().link(request(KAZAN, 2, List.of(), Pets.NONE)).orElseThrow().url())
				.isEqualTo("https://www.avito.ru/kazan/kvartiry/sdam/posutochno-ASgBAgICAkSSA8gQ8AeSUg");
		assertThat(new CianSite().link(request(KAZAN, 2, List.of(), Pets.NONE)).orElseThrow().url())
				.isEqualTo("https://kazan.cian.ru/snyat-kvartiru-posutochno/");

		assertThat(new AvitoSite().link(request(ROME, 2, List.of(), Pets.NONE))).isEmpty();
		assertThat(new CianSite().link(request(ROME, 2, List.of(), Pets.NONE))).isEmpty();
	}

	@Test
	void avitoAndCianApplyNoFilters() {
		assertThat(new AvitoSite().link(request(KAZAN, 2, List.of(), Pets.DOG)).orElseThrow().applied()).isEmpty();
	}

	private static LodgingRequest request(KnownCity city, int adults, List<Integer> ages, Pets pets) {
		return new LodgingRequest(city.name(), Optional.of(city), IN, OUT, adults, ages, pets, Budget.ANY);
	}

	private static LodgingRequest unknown(String cityText) {
		return new LodgingRequest(cityText, Optional.empty(), IN, OUT, 2, List.of(), Pets.NONE, Budget.ANY);
	}
}
